/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

rootProject.name = "build-logic"

pluginManagement {
    repositories {
        maven("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2") {
            name = "MavenCentral-JBCache"
        }
        maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2") {
            name = "GradlePluginPortal-JBCache"
        }
    }
    includeBuild("../build-settings-logic")
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        maven("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2") {
            name = "MavenCentral-JBCache"
        }
        maven("https://cache-redirector.jetbrains.com/dl.google.com.android.maven2") {
            name = "Google-JBCache"
        }
        maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2") {
            name = "GradlePluginPortal-JBCache"
        }
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
    id("dokkasettings.build-cache")
}
