/*
 * Copyright (c) 2011-2022 VMware Inc. or its affiliates, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.diffplug.gradle.spotless.SpotlessExtension
import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.miret.etienne.gradle.sass.CompileSass
import java.util.concurrent.TimeUnit

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
}

plugins {
    java
    application
    alias(libs.plugins.shadow)
    alias(libs.plugins.sass)
    alias(libs.plugins.spotless)
}

group = "io.projectreactor"
version = "1.0.0.BUILD-SNAPSHOT"

var isCiServer = System.getenv().containsKey("CI")

configure<JavaApplication> {
    mainClass.set("io.projectreactor.Application")
}

configure<ShadowExtension> {
	version = ""
}

tasks.withType<CompileSass> {
    style = compressed
    //cannot set sourceDir, but default is $projectDir/src/main/sass - phew!
    outputDir = file("$buildDir/resources/main/static/assets/css/")
}

tasks.withType<Jar> {
    val compileSass = tasks.getByName("compileSass")
    dependsOn(compileSass)
    from(file("$buildDir/resources/main/static/assets/css/"))
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    archiveVersion.set("")
}

configure<SpotlessExtension> {
    if (project.hasProperty("spotlessFrom")) {
        val spotlessBranch = project.properties["spotlessFrom"].toString()
        if (spotlessBranch == "ALL") {
            println("[Spotless] Ratchet deactivated")
        }
        else {
            println("[Spotless] Ratchet from $spotlessBranch")
            ratchetFrom(spotlessBranch)
        }
    }
    else if (isCiServer) {
        println ("[Spotless] CI detected without explicit branch, not enforcing check")
        isEnforceCheck = false
    }
    else {
        val spotlessBranch = "origin/main"
        println("[Spotless] Local run detected, ratchet from $spotlessBranch")
        ratchetFrom(spotlessBranch)
    }

    java {
        target("**/*.java")
        licenseHeaderFile("codequality/spotless/licenseSlashstarStyle.txt")
    }
}

repositories {
    mavenCentral()
    maven { setUrl("https://repo.spring.io/milestone") }
    mavenLocal()
    maven { setUrl("https://repo.spring.io/snapshot") }
}

dependencies {
    implementation(platform(libs.reactorBom))
    implementation("io.projectreactor.netty:reactor-netty")
    implementation("io.projectreactor:reactor-core")
    implementation(libs.jsr305)
    implementation(libs.spring)
    implementation(libs.thymeleaf)
    implementation(libs.snakeYaml)
    implementation(libs.jackson)
    runtimeOnly(libs.commonsLogging)
    runtimeOnly(libs.slf4jApi)
    runtimeOnly(libs.logback)

    testImplementation(libs.junit4)
    testImplementation(libs.assertJ)
}
