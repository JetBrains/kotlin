/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
    gradlePluginPortal()
}

kotlin {
    jvm()
    linuxX64("linux")
    macosX64("macos")
    js(IR) // Starting with Kotlin 1.9.0, using compiler types LEGACY or BOTH leads to an error.
    //TODO Add wasm when kx.coroutines will be supported and published into the main repo
    sourceSets {
        val commonMain by sourceSets.getting
        val linuxMain by sourceSets.getting
        val macosMain by sourceSets.getting
        val desktopMain by sourceSets.creating {
            dependsOn(commonMain)
            linuxMain.dependsOn(this)
            macosMain.dependsOn(this)
        }
    }
}

