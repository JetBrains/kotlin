/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

pluginManagement {
    val pluginBootstrapVersion = java.util.Properties().apply {
        file("../../gradle.properties").inputStream().use { load(it) }
    }.getProperty("bootstrap.kotlin.default.version")
        ?: error("Missing bootstrap.kotlin.default.version")
    plugins {
        id("org.jetbrains.kotlin.jvm") version pluginBootstrapVersion
    }
    repositories {
        maven("https://cache-redirector.jetbrains.com/redirector.kotlinlang.org/maven/bootstrap")
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://cache-redirector.jetbrains.com/redirector.kotlinlang.org/maven/bootstrap")
        mavenCentral()
    }
}

rootProject.name = "bootstrap-native-utils"
