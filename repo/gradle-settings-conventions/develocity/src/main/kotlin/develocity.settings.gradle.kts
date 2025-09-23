plugins {
    id("com.gradle.develocity")
    id("com.gradle.common-custom-user-data-gradle-plugin") apply false
}

val buildProperties = getKotlinBuildPropertiesForSettings(settings)

val buildScanServer = buildProperties.buildScanServer

if (!buildScanServer.isNullOrEmpty()) {
    plugins.apply("com.gradle.common-custom-user-data-gradle-plugin")
}

develocity {
    val isTeamCity = buildProperties.isTeamcityBuild
    val hasBuildScanServer = !buildScanServer.isNullOrEmpty()
    if (hasBuildScanServer) {
        server.set(buildScanServer)
    }
    buildScan {
        capture {
            uploadInBackground.set(!isTeamCity)
        }
        publishing.onlyIf { hasBuildScanServer }

        val overriddenUsername = (buildProperties.getOrNull("kotlin.build.scan.username") as? String)?.trim()
        val overriddenHostname = (buildProperties.getOrNull("kotlin.build.scan.hostname") as? String)?.trim()
        if (buildProperties.isJpsBuildEnabled) {
            tag("JPS")
        }
        obfuscation {
            ipAddresses { _ -> listOf("0.0.0.0") }
            hostname { _ -> overriddenHostname ?: "concealed" }
            username { originalUsername ->
                when {
                    isTeamCity -> "TeamCity"
                    overriddenUsername.isNullOrEmpty() -> "concealed"
                    overriddenUsername == "<default>" -> originalUsername
                    else -> overriddenUsername
                }
            }
        }
    }
}

buildCache {
    local {
        isEnabled = buildProperties.localBuildCacheEnabled
        if (buildProperties.localBuildCacheDirectory != null) {
            directory = buildProperties.localBuildCacheDirectory
        }
    }
    if (develocity.server.isPresent) {
        val remoteBuildCacheUrl = buildProperties.buildCacheUrl?.trim()
        if (System.getenv("TC_K8S_CLOUD_PROFILE_ID") == "kotlindev-kotlin-k8s") {
            remote(develocity.buildCache) {
                isPush = buildProperties.pushToBuildCache
                isEnabled = remoteBuildCacheUrl != "" // explicit "" disables it
                server = "https://kotlin-cache.eqx.k8s.intellij.net"
            }
        } else {
            remote(develocity.buildCache) {
                isPush = buildProperties.pushToBuildCache
                isEnabled = remoteBuildCacheUrl != "" // explicit "" disables it
                if (!remoteBuildCacheUrl.isNullOrEmpty()) {
                    server = remoteBuildCacheUrl.removeSuffix("/cache/")
                }
            }
        }
    }
}