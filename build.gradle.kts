import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.robfletcher.compass.CompassExtension
import java.util.concurrent.TimeUnit

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
}

buildscript {
    repositories {
        mavenLocal()
        jcenter()
        maven { setUrl("http://dl.bintray.com/robfletcher/gradle-plugins") }
        maven { setUrl("https://repo.spring.io/release") }
        maven { setUrl("https://repo.spring.io/snapshot") }
        mavenCentral()
    }
    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.4")
        classpath("com.github.robfletcher:compass-gradle-plugin:2.0.6")
    }
}

apply {
    plugin("java")
    plugin("com.github.robfletcher.compass")
    plugin("application")
    plugin("com.github.johnrengelman.shadow")
}

group = "io.projectreactor"
version = "1.0.0.BUILD-SNAPSHOT"

configure<JavaPluginConvention> {
    setSourceCompatibility(1.8)
    setTargetCompatibility(1.8)
}

configure<ApplicationPluginConvention> {
	mainClassName = "io.projectreactor.Application"
}

configure<ShadowExtension> {
	version = null
}

configure<CompassExtension> {
    sassDir = file("$projectDir/src/main/sass")
    cssDir = file("$buildDir/resources/main/static/assets/css")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { setUrl("http://repo.spring.io/release") }
    maven { setUrl("http://repo.spring.io/milestone") }
    maven { setUrl("https://repo.spring.io/snapshot") }
}

dependencies {
    compile("org.springframework:spring-core:5.0.0.RC1")
    compile("io.projectreactor.ipc:reactor-netty:0.6.3.RELEASE")
    compile("io.projectreactor:reactor-core:3.1.0.M2")
    compile("org.yaml:snakeyaml:1.17")
    runtime("commons-logging:commons-logging:1.2")
    runtime("org.slf4j:slf4j-api:1.7.21")
    runtime("ch.qos.logback:logback-classic:1.1.7")

    testCompile("junit:junit:4.12")
    testCompile("org.assertj:assertj-core:3.6.1")
}

val processResources = tasks.getByName("processResources")
val compassCompile = tasks.getByName("compassCompile")
processResources.dependsOn(compassCompile)
