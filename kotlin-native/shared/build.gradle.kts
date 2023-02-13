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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("kotlin")
}

val rootBuildDirectory by extra(file(".."))
apply(from = "../gradle/loadRootProperties.gradle")

val kotlinVersion = project.bootstrapKotlinVersion

group = "org.jetbrains.kotlin"

repositories {
    maven("https://cache-redirector.jetbrains.com/maven-central")
    mavenCentral()
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

tasks.jar {
    archiveFileName.set("shared.jar")
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    useJUnitPlatform()
}

dependencies {
    kotlinCompilerClasspath("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")

    implementation(kotlinStdlib())
    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    api("org.jetbrains.kotlin:kotlin-native-utils:$kotlinVersion")
    api(project(":kotlin-util-klib"))
    testApiJUnit5()
}
