/*
 * Copyright 2016-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

plugins {
    kotlin("multiplatform") version "1.9.10"
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

repositories {
    mavenCentral()
}

kotlin {
    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
    }
}

apiValidation {
    klib.enabled = true
}
