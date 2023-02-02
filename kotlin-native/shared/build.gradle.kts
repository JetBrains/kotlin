/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("kotlin")
}

val rootBuildDirectory by extra(file(".."))
apply(from = "../gradle/loadRootProperties.gradle")

group = "org.jetbrains.kotlin"

repositories {
    maven("https://cache-redirector.jetbrains.com/maven-central")
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir("src/main/kotlin")
            kotlin.srcDir("src/library/kotlin")
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += listOf("-Xskip-prerelease-check")
}

// TODO: move it somewhere
//projectTest(jUnitMode = JUnitMode.JUnit5) {
//    useJUnitPlatform()
//}

/**
 * Depending on the `kotlin.native.build.composite-bootstrap` property returns either coordinates or project dependency.
 * This is to use this project in composite build (build-tools) and as a project itself.
 * Project should depend on a current snapshot builds while build-tools use bootstrap dependencies
 */
fun compositeDependency(coordinates: String, subproject: String = ""): Any {
    val bootstrap = project.extraProperties.has("kotlin.native.build.composite-bootstrap")
    val parts = coordinates.split(':')
    check(parts.size == 3) {
        "Full dependency coordinates should be specified group:name:version"
    }
    return if (!bootstrap) {
        // returns dependency on the project specified with coordinates
        dependencies.project("$subproject:${parts[1]}")
    } else {
        // returns full coordinates
        coordinates
    }
}

dependencies {
    kotlinCompilerClasspath("org.jetbrains.kotlin:kotlin-compiler-embeddable:${project.bootstrapKotlinVersion}")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:${project.bootstrapKotlinVersion}")
    api(compositeDependency("org.jetbrains.kotlin:kotlin-native-utils:${project.bootstrapKotlinVersion}", ":native"))
    api(compositeDependency("org.jetbrains.kotlin:kotlin-util-klib:${project.bootstrapKotlinVersion}"))
    api(compositeDependency("org.jetbrains.kotlin:kotlin-util-io:${project.bootstrapKotlinVersion}"))
//    testApiJUnit5()
}
