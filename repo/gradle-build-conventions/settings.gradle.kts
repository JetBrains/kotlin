import java.util.*

pluginManagement {
    apply(from = "../scripts/cache-redirector.settings.gradle.kts")
    apply(from = "../scripts/kotlin-bootstrap.settings.gradle.kts")

    includeBuild("../gradle-settings-conventions")

    repositories {
        maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

plugins {
    id("build-cache")
    id("gradle-enterprise")
    id("jvm-toolchain-provisioning")
    id("kotlin-daemon-config")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

include(":buildsrc-compat")
include(":prepare-deps")
