import com.github.robfletcher.compass.CompassExtension
import org.springframework.boot.gradle.SpringBootPluginExtension
import java.util.concurrent.TimeUnit

configurations.all {
    it.resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
}

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
    // TODO Remove the spring-context-support dependency when https://jira.spring.io/browse/SPR-14908 will be fixed
    compile("org.springframework:spring-context-support:5.0.0.BUILD-SNAPSHOT")
    compile("io.projectreactor.ipc:reactor-netty:0.6.0.BUILD-SNAPSHOT")
    compile("org.yaml:snakeyaml:1.17")
    runtime("commons-logging:commons-logging:1.2")
    runtime("org.slf4j:slf4j-api:1.7.21")
    runtime("ch.qos.logback:logback-classic:1.1.7")

}

val processResources = tasks.getByName("processResources")
val compassCompile = tasks.getByName("compassCompile")
processResources.dependsOn(compassCompile)
