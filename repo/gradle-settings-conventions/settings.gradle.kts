pluginManagement {
    apply(from = "cache-redirector/src/main/kotlin/cache-redirector.settings.gradle.kts")
    apply(from = "kotlin-bootstrap/src/main/kotlin/kotlin-bootstrap.settings.gradle.kts")

    repositories {
        maven(url = "https://redirector.kotlinlang.org/maven/kotlin-dependencies")
        mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
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
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("com.gradle.develocity") version("4.2.2")
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
include(":cache-redirector")
include(":kotlin-bootstrap")

// Sync below to the content of develocity settings plugin
val buildProperties = settings.kotlinBuildProperties

develocity {
    server.set(buildProperties.buildScanServer)
    buildScan {
        capture {
            uploadInBackground.set(buildProperties.isTeamcityBuild.map { !it })
        }

        val overriddenUsername = buildProperties.stringProperty("kotlin.build.scan.username").map { it.trim() }
        val overriddenHostname = buildProperties.stringProperty("kotlin.build.scan.hostname").map { it.trim() }
        obfuscation {
            ipAddresses { _ -> listOf("0.0.0.0") }
            hostname { _ -> overriddenHostname.orNull ?: "concealed" }
            username { originalUsername ->
                when {
                    buildProperties.isTeamcityBuild.get() -> "TeamCity"
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
