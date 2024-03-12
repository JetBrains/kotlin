/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Gradle Build Cache conventions.
 *
 * Because Dokka uses Composite Builds, Build Cache must be configured consistently on
 * - the root settings.gradle.kts,
 * - and the settings.gradle.kts of any projects added with `pluginManagement { includedBuild("...") }`
 *
 * See https://docs.gradle.org/8.4/userguide/build_cache.html#sec:build_cache_composite.
 *
 * ⚠️ This file _must_ be applicable as a script plugin and so _must not_ depend on other source files.
 *
 * Based on https://github.com/JetBrains/kotlin/blob/2675531624d42851af502a993bbefd65ee3e38ef/repo/gradle-settings-conventions/build-cache/src/main/kotlin/build-cache.settings.gradle.kts
 */

val buildingOnTeamCity: Boolean = System.getenv("TEAMCITY_VERSION") != null
val buildingOnGitHub: Boolean = System.getenv("GITHUB_ACTION") != null
val buildingOnCi: Boolean = System.getenv("CI") != null || buildingOnTeamCity || buildingOnGitHub

fun dokkaProperty(name: String): Provider<String> =
    providers.gradleProperty("org.jetbrains.dokka.$name")

fun <T : Any> dokkaProperty(name: String, convert: (String) -> T): Provider<T> =
    dokkaProperty(name).map(convert)

//region Gradle Build Cache
val buildCacheLocalEnabled: Provider<Boolean> =
    dokkaProperty("build.cache.local.enabled", String::toBoolean)
        .orElse(!buildingOnCi)
val buildCacheLocalDirectory: Provider<String> =
    dokkaProperty("build.cache.local.directory")
val buildCacheUrl: Provider<String> =
    dokkaProperty("build.cache.url").map(String::trim)
val buildCachePushEnabled: Provider<Boolean> =
    dokkaProperty("build.cache.push", String::toBoolean)
        .orElse(buildingOnTeamCity)
val buildCacheUser: Provider<String> =
    dokkaProperty("build.cache.user")
val buildCachePassword: Provider<String> =
    dokkaProperty("build.cache.password")
//endregion

buildCache {
    local {
        isEnabled = buildCacheLocalEnabled.get()
        if (buildCacheLocalDirectory.orNull != null) {
            directory = buildCacheLocalDirectory.get()
        }
    }
    remote<HttpBuildCache> {
        url = uri("https://ge.jetbrains.com/cache/")
        isPush = buildCachePushEnabled.get()

        if (buildCacheUser.isPresent &&
            buildCachePassword.isPresent
        ) {
            credentials.username = buildCacheUser.get()
            credentials.password = buildCachePassword.get()
        }
    }
}
