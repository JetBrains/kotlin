/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.gradle.api.initialization.resolve.RepositoriesMode.PREFER_SETTINGS

rootProject.name = "build-settings-logic"

pluginManagement {
    repositories {
        mavenCentral {
            setUrl("https://cache-redirector.jetbrains.com/maven-central")
            name = "MavenCentral-JBCache"
        }
        maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2") {
            name = "GradlePluginPortal-JBCache"
        }
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode = PREFER_SETTINGS
    repositories {
        mavenCentral {
            setUrl("https://cache-redirector.jetbrains.com/maven-central")
            name = "MavenCentral-JBCache"
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

// We have to make sure the build-cache config is consistent in the settings.gradle.kts
// files of all projects. The natural way to share logic is with a convention plugin,
// but we can't apply a convention plugin in build-settings-logic to itself, so we just copy it.
// We could publish build-settings-logic to a Maven repo? But this is quicker and easier.
// The following content should be kept in sync with the content of:
//   `src/main/kotlin/dokkasettings.settings.gradle.kts`
// The only difference with the script above is explicitly specified versions

//region copy of src/main/kotlin/dokkasettings.settings.gradle.kts

// version should be kept in sync with `gradle/libs.versions.toml`
plugins {
    id("com.gradle.develocity") version "3.17.6"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "2.0.2" apply false
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

//region properties
val buildingOnTeamCity: Boolean = System.getenv("TEAMCITY_VERSION") != null
val buildingOnGitHub: Boolean = System.getenv("GITHUB_ACTION") != null
val buildingOnCi: Boolean = System.getenv("CI") != null || buildingOnTeamCity || buildingOnGitHub

fun dokkaProperty(name: String): Provider<String> =
    providers.gradleProperty("org.jetbrains.dokka.$name")

fun <T : Any> dokkaProperty(name: String, convert: (String) -> T): Provider<T> =
    dokkaProperty(name).map(convert)
//endregion

//region Gradle Build Scan
// NOTE: build scan properties are documented in CONTRIBUTING.md
val buildScanEnabled: Provider<Boolean> =
    dokkaProperty("build.scan.enabled", String::toBoolean)
        .orElse(buildingOnCi)

val BUILD_SCAN_USERNAME_DEFAULT = "<default>"

/** Optionally override the default name attached to a Build Scan. */
val buildScanUsername: Provider<String> =
    dokkaProperty("build.scan.username")
        .orElse(BUILD_SCAN_USERNAME_DEFAULT)
        .map(String::trim)

develocity {
    val buildScanEnabled = buildScanEnabled.get()

    if (buildScanEnabled) {
        plugins.apply("com.gradle.common-custom-user-data-gradle-plugin")
    }

    server = "https://ge.jetbrains.com/"

    buildScan {
        publishing {
            onlyIf { buildScanEnabled }
        }

        capture {
            buildLogging = buildScanEnabled
            fileFingerprints = buildScanEnabled
            testLogging = buildScanEnabled
        }

        val overriddenName = buildScanUsername.orNull
        obfuscation {
            ipAddresses { _ -> listOf("0.0.0.0") }
            hostname { _ -> "concealed" }
            username { originalUsername ->
                when {
                    buildingOnTeamCity -> "TeamCity"
                    buildingOnGitHub -> "GitHub"
                    buildingOnCi -> "CI"
                    !overriddenName.isNullOrBlank() -> overriddenName
                    overriddenName == BUILD_SCAN_USERNAME_DEFAULT -> originalUsername
                    else -> "unknown"
                }
            }
        }
    }
}
//endregion

//region Gradle Build Cache
val buildCacheLocalEnabled: Provider<Boolean> =
    dokkaProperty("build.cache.local.enabled", String::toBoolean)
        .orElse(!buildingOnCi)
val buildCacheLocalDirectory: Provider<String> =
    dokkaProperty("build.cache.local.directory")
val buildCachePushEnabled: Provider<Boolean> =
    dokkaProperty("build.cache.push", String::toBoolean)
        .orElse(buildingOnCi)

buildCache {
    local {
        isEnabled = buildCacheLocalEnabled.get()
        if (buildCacheLocalDirectory.orNull != null) {
            directory = buildCacheLocalDirectory.get()
        }
    }
    remote(develocity.buildCache) {
        isPush = buildCachePushEnabled.get()
    }
}
//endregion

//endregion
