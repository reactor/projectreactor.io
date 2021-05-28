/*
 * Copyright (c) 2011-Present Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.salomonbrys.gradle.sass.SassTask
import java.util.concurrent.TimeUnit

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
}

plugins {
    java
    application
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("com.github.salomonbrys.gradle.sass") version "1.2.0"
}

group = "io.projectreactor"
version = "1.0.0.BUILD-SNAPSHOT"

configure<ApplicationPluginConvention> {
	mainClassName = "io.projectreactor.Application"
}

configure<ShadowExtension> {
	version = ""
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    archiveVersion.set("")
}

configure<SassTask> {
    style = compressed
    source = fileTree("$projectDir/src/main/sass")
    outputDir = file("$buildDir/resources/main/static/assets/css/")
}

repositories {
    mavenCentral()
    maven { setUrl("https://repo.spring.io/milestone") }
    mavenLocal()
    maven { setUrl("https://repo.spring.io/snapshot") }
}

dependencies {
    implementation(platform("io.projectreactor:reactor-bom:2020.0.7"))
    implementation("io.projectreactor.netty:reactor-netty")
    implementation("io.projectreactor:reactor-core")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("org.springframework:spring-core:5.3.7")
    implementation("org.thymeleaf:thymeleaf:3.0.12.RELEASE")
    implementation("org.yaml:snakeyaml:1.28")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.3")
    runtimeOnly("commons-logging:commons-logging:1.2")
    runtimeOnly("org.slf4j:slf4j-api:1.7.30")
    runtimeOnly("ch.qos.logback:logback-classic:1.2.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.assertj:assertj-core:3.19.0")
}

val processResources = tasks.getByName("processResources")
val sassCompile = tasks.getByName("sassCompile")
processResources.dependsOn(sassCompile)
