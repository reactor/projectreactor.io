import com.github.robfletcher.compass.CompassExtension
import org.springframework.boot.gradle.SpringBootPluginExtension
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.client.reactive.ClientRequest
import org.springframework.web.client.reactive.WebClient
import java.io.*
import java.nio.ByteBuffer
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

val artifacts = listOf(
        Triple("io.projectreactor", "reactor-core", "3.0.3.RELEASE"),
        Triple("io.projectreactor.addons", "reactor-test", "3.0.3.RELEASE"),
        Triple("io.projectreactor.addons", "reactor-adapter", "3.0.3.RELEASE"),
        Triple("io.projectreactor.ipc", "reactor-netty", "0.5.2.RELEASE")
)

buildscript {
    repositories {
        mavenLocal()
        jcenter()
        maven { setUrl("http://dl.bintray.com/robfletcher/gradle-plugins") }
        maven { setUrl("https://repo.spring.io/snapshot") }
        mavenCentral()
    }
    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:2.0.0.BUILD-SNAPSHOT")
        classpath("com.github.robfletcher:compass-gradle-plugin:2.0.6")
        classpath("org.springframework:spring-web:5.0.0.BUILD-SNAPSHOT")
        classpath("io.projectreactor.ipc:reactor-netty:0.6.0.BUILD-SNAPSHOT")
    }
}

apply {
    plugin("java")
    plugin("com.github.robfletcher.compass")
    plugin("org.springframework.boot")
}

group = "io.projectreactor"
version = "1.0.0.BUILD-SNAPSHOT"

configure<JavaPluginConvention> {
    setSourceCompatibility(1.8)
    setTargetCompatibility(1.8)
}

configure<SpringBootPluginExtension> {
    mainClass = "io.projectreactor.Application"
}

configure<CompassExtension> {
    sassDir = file("$projectDir/src/main/sass")
    cssDir = file("$buildDir/resources/main/static/assets/css")
}


repositories {
    mavenLocal()
    mavenCentral()
    maven { setUrl("http://repo.spring.io/libs-milestone") }
    maven { setUrl("https://repo.spring.io/snapshot") }
}

dependencies {
    compile("org.springframework:spring-web-reactive:5.0.0.BUILD-SNAPSHOT")
    // TODO Remove the spring-context-support dependency when https://jira.spring.io/browse/SPR-14908 will be fixed
    compile("org.springframework:spring-context-support:5.0.0.BUILD-SNAPSHOT")
    compile("io.projectreactor.ipc:reactor-netty:0.6.0.BUILD-SNAPSHOT")
    runtime("commons-logging:commons-logging:1.2")
    runtime("org.slf4j:slf4j-api:1.7.21")
    runtime("ch.qos.logback:logback-classic:1.1.7")
}

val docsGenerate = task("docsGenerate") {
    doLast {
        println("Download and generate Javadoc")
        val outputDir = ("$buildDir/resources/main/static")
        val webClient = WebClient.builder(ReactorClientHttpConnector()).build()
        val bufferFactory = DefaultDataBufferFactory()
        for (artifact in artifacts) {
            val groupId = artifact.first.replace('.', '/')
            val artifactId = artifact.second
            val version = artifact.third
            val url = "http://repo.spring.io/release/$groupId/$artifactId/$version/$artifactId-$version-javadoc.jar"
            println("Downloading Javadoc from: $url")
            val clientRequest = ClientRequest.GET(url).build()
            val inputStream = webClient.exchange(clientRequest)
                    .flatMap { it.bodyToFlux(ByteBuffer::class.java) }
                    // TODO Remove when SPR-14918 will be fixed
                    .map { bufferFactory.wrap(it) }
                    // TODO Maybe a Reactor issue on too strict generic parameter on reduce operator
                    .cast(DataBuffer::class.java)
                    .reduce { buffer1, buffer2 -> buffer1.write(buffer2) }
                    .map { it.asInputStream() }
                    .doOnError { println("Error for $artifactId-$version-javadoc.jar: $it") }
                    .block()
            unzip(inputStream, "$outputDir/${artifactId.replace("reactor-", "")}/docs/api/")
        }
    }
}

fun unzip(zip: InputStream, outputDir: String): Unit {
    val destDir: File = File(outputDir)
    if (destDir.exists()) destDir.deleteRecursively() else destDir.mkdirs()
    val zipIn: ZipInputStream = ZipInputStream(zip)
    var entry: ZipEntry? = zipIn.nextEntry
    while (entry != null) {
        val filePath: String = outputDir + File.separator + entry.name
        if (!entry.isDirectory) {
            val bos: BufferedOutputStream = BufferedOutputStream(FileOutputStream(filePath))
            bos.write(zipIn.readBytes())
            bos.close()
        } else {
            val dir: File = File(filePath)
            dir.mkdirs()
        }
        zipIn.closeEntry()
        entry = zipIn.nextEntry
    }
    zipIn.close()
}

val processResources = tasks.getByName("processResources")
val compassCompile = tasks.getByName("compassCompile")
processResources.dependsOn(compassCompile)
