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
            uploadInBackground = !isTeamCity
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
        remote(develocity.buildCache) {
            isPush = buildProperties.pushToBuildCache
            val remoteBuildCacheUrl = buildProperties.buildCacheUrl?.trim()
            isEnabled = remoteBuildCacheUrl != "" // explicit "" disables it
            if (!remoteBuildCacheUrl.isNullOrEmpty()) {
                server = remoteBuildCacheUrl.removeSuffix("/cache/")
            }
        }
    }
}