import kotlin.io.path.createFile
import kotlin.io.path.exists

plugins {
    id("com.gradle.develocity")
    id("com.gradle.common-custom-user-data-gradle-plugin") apply false
}

val buildProperties = settings.kotlinBuildProperties

if (buildProperties.buildScanServer.isPresent) {
    plugins.apply("com.gradle.common-custom-user-data-gradle-plugin")
}

develocity {
    val buildScanServer = buildProperties.buildScanServer
    server.set(buildScanServer)
    buildScan {
        publishing {
            onlyIf { buildScanServer.isPresent }
        }
        val isTeamcityBuild = buildProperties.isTeamcityBuild
        val overriddenUsername = buildProperties.stringProperty("kotlin.build.scan.username").map { it.trim() }
        val overriddenHostname = buildProperties.stringProperty("kotlin.build.scan.hostname").map { it.trim() }

        capture {
            uploadInBackground.set(isTeamcityBuild.map { !it })
        }

        /*
        Indicate if a teamcity agent is considered 'cold' or 'warm'
        'cold': The agent has never executed any Gradle build invocation prior
        'warm': The agent has completed at least one Gradle build invocation, subsequent builds are supposed to be faster
        */
        val warmAgentMarker = gradle.gradleUserHomeDir.resolve(".tc.agent.warm.marker").toPath()
        background {
            if (isTeamcityBuild.get()) {
                if (!warmAgentMarker.exists()) {
                    warmAgentMarker.createFile()
                    value("Agent State", "cold")
                } else {
                    value("Agent State", "warm")
                }
            }
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
        val remoteBuildCacheUrl = buildProperties.buildCacheUrl.orNull?.trim()
        if (System.getenv("TC_K8S_CLOUD_PROFILE_ID") == "kotlindev-kotlin-k8s") {
            remote(develocity.buildCache) {
                isPush = buildProperties.pushToBuildCache.get()
                isEnabled = remoteBuildCacheUrl != "" // explicit "" disables it
                server = "https://kotlin-cache.eqx.k8s.intellij.net"
            }
        } else {
            remote(develocity.buildCache) {
                isPush = buildProperties.pushToBuildCache.get()
                isEnabled = remoteBuildCacheUrl != "" // explicit "" disables it
                if (!remoteBuildCacheUrl.isNullOrEmpty()) {
                    server = remoteBuildCacheUrl.removeSuffix("/cache/")
                }
            }
        }
    }
}
