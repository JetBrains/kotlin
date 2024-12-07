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
    id("develocity")
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