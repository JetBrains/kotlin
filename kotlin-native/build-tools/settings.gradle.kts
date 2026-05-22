rootProject.name = "native-build-tools"

pluginManagement {
    includeBuild("../../repo/kotlin-build-helpers")
    includeBuild("../../repo/gradle-settings-conventions")

    repositories {
        maven("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
        mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
        gradlePluginPortal()
    }
}

plugins {
    id("kotlin-build-helpers")
    id("kotlin-bootstrap")
    id("jvm-toolchain-provisioning")
    id("develocity")
    id("kotlin-daemon-config")
    id("cache-redirector")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}
