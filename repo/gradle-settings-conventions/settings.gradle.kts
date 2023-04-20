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
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
}

include(":build-cache")
include(":gradle-enterprise")
include(":jvm-toolchain-provisioning")
include(":kotlin-daemon-config")
include(":internal-gradle-setup")

// Unfortunately it is not possible to apply build-cache.settings.gradle.kts as script compilation
// could not then find types from "kotlin-build-gradle-plugin"
// Sync below to the content of settings plugin
val buildProperties = getKotlinBuildPropertiesForSettings(settings)

buildCache {
    local {
        isEnabled = buildProperties.localBuildCacheEnabled
        if (buildProperties.localBuildCacheDirectory != null) {
            directory = buildProperties.localBuildCacheDirectory
        }
    }

    val remoteBuildCacheUrl = buildProperties.buildCacheUrl
    if (remoteBuildCacheUrl != null) {
        remote<HttpBuildCache> {
            url = uri(remoteBuildCacheUrl)
            isPush = buildProperties.pushToBuildCache
            if (buildProperties.buildCacheUser != null &&
                buildProperties.buildCachePassword != null
            ) {
                credentials.username = buildProperties.buildCacheUser
                credentials.password = buildProperties.buildCachePassword
            }
        }
    }
}
