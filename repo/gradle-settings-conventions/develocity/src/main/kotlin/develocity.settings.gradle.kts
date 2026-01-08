plugins {
    id("com.gradle.develocity")
    id("com.gradle.common-custom-user-data-gradle-plugin") apply false
}

val buildProperties = settings.kotlinBuildProperties
val buildScanServer = buildProperties.buildScanServer.orNull

if (!buildScanServer.isNullOrEmpty()) {
    plugins.apply("com.gradle.common-custom-user-data-gradle-plugin")
}

develocity {
    server.set(buildProperties.buildScanServer)
    buildScan {
        val isTeamcityBuild = buildProperties.isTeamcityBuild
        val overriddenUsername = buildProperties.stringProperty("kotlin.build.scan.username").map { it.trim() }
        val overriddenHostname = buildProperties.stringProperty("kotlin.build.scan.hostname").map { it.trim() }

        capture {
            uploadInBackground.set(isTeamcityBuild.map { !it })
        }

        obfuscation {
            ipAddresses { _ -> listOf("0.0.0.0") }
            hostname { _ -> overriddenHostname.orNull ?: "concealed" }
            username { originalUsername ->
                when {
                    isTeamcityBuild.get() -> "TeamCity"
                    overriddenUsername.orNull.isNullOrEmpty() -> "concealed"
                    overriddenUsername.orNull == "<default>" -> originalUsername
                    else -> overriddenUsername.orNull
                }
            }
        }
    }
}

buildCache {
    local {
        isEnabled = buildProperties.localBuildCacheEnabled.get()
        if (buildProperties.localBuildCacheDirectory.orNull != null) {
            directory = buildProperties.localBuildCacheDirectory.get()
        }
    }
    if (develocity.server.isPresent) {
        if (System.getenv("TC_K8S_CLOUD_PROFILE_ID") == "kotlindev-kotlin-k8s") {
            remote(develocity.buildCache) {
                isPush = buildProperties.pushToBuildCache.get()
                server = "https://kotlin-cache.eqx.k8s.intellij.net"
            }
        } else {
            remote(develocity.buildCache) {
                isPush = buildProperties.pushToBuildCache.get()
                val remoteBuildCacheUrl = buildProperties.buildCacheUrl.orNull?.trim()
                isEnabled = remoteBuildCacheUrl != "" // explicit "" disables it
                if (!remoteBuildCacheUrl.isNullOrEmpty()) {
                    server = remoteBuildCacheUrl.removeSuffix("/cache/")
                }
            }
        }
    }
}
