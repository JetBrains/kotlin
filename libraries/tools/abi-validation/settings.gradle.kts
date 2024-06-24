/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
import org.gradle.api.initialization.resolve.RepositoriesMode.PREFER_SETTINGS

rootProject.name = "binary-compatibility-validator"


pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.8.0")
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {

    repositoriesMode.set(PREFER_SETTINGS)

    repositories {
        mavenCentral()
        google()
    }
}
