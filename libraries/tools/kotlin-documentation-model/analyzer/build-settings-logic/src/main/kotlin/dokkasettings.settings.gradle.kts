/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

// Based on https://github.com/JetBrains/kotlin/blob/c20f644ee4cd8d28b39b12ea5304b68c5639e531/repo/gradle-settings-conventions/develocity/src/main/kotlin/develocity.settings.gradle.kts
// Because Dokka uses Composite Builds, Build Cache must be configured consistently on:
// - the root settings.gradle.kts,
// - and the settings.gradle.kts of any projects added with `pluginManagement { includedBuild("...") }`
// The Content of this file should be kept in sync with the content at the end of:
//   `build-settings-logic/settings.gradle.kts`
// useful links:
// - develocity: https://docs.gradle.com/develocity/gradle-plugin/
// - build cache: https://docs.gradle.org/8.4/userguide/build_cache.html#sec:build_cache_composite

plugins {
    id("com.gradle.develocity")
    id("com.gradle.common-custom-user-data-gradle-plugin") apply false
    id("org.gradle.toolchains.foojay-resolver-convention")
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

/** Optionally override the default name attached to a Build Scan. */
val buildScanUsername: Provider<String> =
    dokkaProperty("build.scan.username")
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
        // we need to assign properties' values to local variables to avoid issues with Gradle Configuration Cache.
        // if we don't do it, by using those properties in `username { ... }` we catch `this` in closure
        // and Gradle Configuration Cache doesn't allow to catch implicit `this` object
        val buildingOnTeamCity = buildingOnTeamCity
        val buildingOnGitHub = buildingOnGitHub
        val buildingOnCi = buildingOnCi
        obfuscation {
            ipAddresses { _ -> listOf("0.0.0.0") }
            hostname { _ -> "concealed" }
            username { originalUsername ->
                when {
                    buildingOnTeamCity -> "TeamCity"
                    buildingOnGitHub -> "GitHub"
                    buildingOnCi -> "CI"
                    !overriddenName.isNullOrBlank() -> overriddenName
                    else -> originalUsername
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
