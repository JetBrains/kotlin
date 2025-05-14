pluginManagement {
    includeBuild("../gradle-settings-conventions")

    repositories {
        maven(url = "file:///dump")
        maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
        mavenCentral()
        google()
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
include(":compiler-tests-convention")
include(":android-sdk-provisioner")
include(":asm-deprecating-transformer")
include(":binary-compatibility-extended")
include(":gradle-plugins-documentation")
include(":gradle-plugins-common")
include(":d8-configuration")
include(":binaryen-configuration")
include(":nodejs-configuration")