/*
 * Copyright 2016-2023 JetBrains s.r.o.
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

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {

    repositoriesMode.set(PREFER_SETTINGS)

    repositories {
        mavenCentral()
        google()
    }
}
