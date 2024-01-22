/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import DokkaBuildSettingsProperties.Companion.dokkaBuildSettingsProperties

/**
 * Gradle Build Cache conventions.
 *
 * See [DokkaBuildSettingsProperties] for properties.
 *
 * Because Dokka uses Composite Builds, Build Cache should only be configured on the root `settings.gradle.kts`.
 * See https://docs.gradle.org/8.4/userguide/build_cache.html#sec:build_cache_composite.
 *
 * Based on https://github.com/JetBrains/kotlin/blob/2675531624d42851af502a993bbefd65ee3e38ef/repo/gradle-settings-conventions/build-cache/src/main/kotlin/build-cache.settings.gradle.kts
 */

val buildSettingsProps = dokkaBuildSettingsProperties

buildCache {
    local {
        isEnabled = buildSettingsProps.buildCacheLocalEnabled.get()
        if (buildSettingsProps.buildCacheLocalDirectory.orNull != null) {
            directory = buildSettingsProps.buildCacheLocalDirectory.get()
        }
    }

    val remoteBuildCacheUrl = buildSettingsProps.buildCacheUrl.orNull
    if (!remoteBuildCacheUrl.isNullOrEmpty()) {
        remote<HttpBuildCache> {
            url = uri(remoteBuildCacheUrl)
            isPush = buildSettingsProps.buildCachePushEnabled.get()

            if (buildSettingsProps.buildCacheUser.isPresent &&
                buildSettingsProps.buildCachePassword.isPresent
            ) {
                credentials.username = buildSettingsProps.buildCacheUser.get()
                credentials.password = buildSettingsProps.buildCachePassword.get()
            }
        }
    }
}
