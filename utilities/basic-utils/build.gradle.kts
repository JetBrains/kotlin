/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    apply(from = "$rootDir/gradle/kotlinGradlePlugin.gradle")
}

plugins {
    kotlin("jvm")
}

tasks.named<KotlinCompile>("compileKotlin") {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xskip-metadata-version-check")
    }
}

// Convert to normal strings since originally these properties contain GStrings.
val kotlinCompilerModule = rootProject.ext["kotlinCompilerModule"].toString()
val kotlinStdLibModule = rootProject.ext["kotlinStdLibModule"].toString()

dependencies {
    api(kotlinStdLibModule)
    implementation(kotlinCompilerModule)
}
