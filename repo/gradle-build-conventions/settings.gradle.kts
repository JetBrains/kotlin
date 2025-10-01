pluginManagement {
    includeBuild("../gradle-settings-conventions")

    repositories {
        mavenLocal()
        maven(url = "https://redirector.kotlinlang.org/maven/kotlin-dependencies")
        mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
        google { setUrl("https://cache-redirector.jetbrains.com/dl.google.com/dl/android/maven2") }
        gradlePluginPortal()
    }
}

plugins {
    id("kotlin-bootstrap")
    id("develocity")
    id("jvm-toolchain-provisioning")
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

include(":buildsrc-compat")
include(":prepare-deps")
include(":generators")
include(":project-tests-convention")
include(":android-sdk-provisioner")
include(":asm-deprecating-transformer")
include(":binary-compatibility-extended")
include(":gradle-plugins-documentation")
include(":gradle-plugins-common")
include(":d8-configuration")
include(":binaryen-configuration")
include(":nodejs-configuration")
include(":utilities")
