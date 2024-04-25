/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.gradle.api.initialization.resolve.RepositoriesMode.PREFER_SETTINGS

rootProject.name = "build-settings-logic"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode = PREFER_SETTINGS
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

// We have to make sure the build-cache config is consistent in the settings.gradle.kts
// files of all projects. The natural way to share logic is with a convention plugin,
// but we can't apply a convention plugin in build-settings-logic to itself, unless we
// do it with a dumb hack:
apply(from = "src/main/kotlin/dokkasettings.build-cache.settings.gradle.kts")
// We could publish build-settings-logic to a Maven repo? But this is quicker and easier.
