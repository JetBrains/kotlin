pluginManagement {
    apply(from = "../scripts/cache-redirector.settings.gradle.kts")
    apply(from = "../scripts/kotlin-bootstrap.settings.gradle.kts")

    repositories {
        maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
        mavenCentral()
        gradlePluginPortal()
    }
}

buildscript {
    val buildGradlePluginVersion = extra.get("kotlin.build.gradlePlugin.version")
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-build-gradle-plugin:$buildGradlePluginVersion")
    }
}

plugins {
    // Versions here should be also synced with the versions in 'libs.versions.toml'
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
    id("com.gradle.develocity") version("3.17.5")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

include(":develocity")
include(":jvm-toolchain-provisioning")
include(":kotlin-daemon-config")
include(":internal-gradle-setup")

// Sync below to the content of develocity settings plugin
val buildProperties = getKotlinBuildPropertiesForSettings(settings)

develocity {
    val isTeamCity = buildProperties.isTeamcityBuild
    if (!buildProperties.buildScanServer.isNullOrEmpty()) {
        server.set(buildProperties.buildScanServer)
    }
    buildScan {
        capture {
            uploadInBackground = !isTeamCity
        }

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
            if (!remoteBuildCacheUrl.isNullOrEmpty()) {
                server = remoteBuildCacheUrl.removeSuffix("/cache/")
                if (buildProperties.buildCacheUser != null) {
                    usernameAndPassword(
                        buildProperties.buildCacheUser,
                        buildProperties.buildCachePassword
                    )
                }
            }
        }
    }
}