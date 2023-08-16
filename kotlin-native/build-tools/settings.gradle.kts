rootProject.name = "native-build-tools"

pluginManagement {
    apply(from = "../../repo/scripts/cache-redirector.settings.gradle.kts")
    apply(from = "../../repo/scripts/kotlin-bootstrap.settings.gradle.kts")

    includeBuild("../../repo/gradle-settings-conventions")

    repositories {
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("jvm-toolchain-provisioning")
    id("build-cache")
    id("kotlin-daemon-config")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

buildscript {
    val buildGradlePluginVersion = extra["kotlin.build.gradlePlugin.version"]
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-build-gradle-plugin:$buildGradlePluginVersion")
    }
}

// This code could potentially remove the need of bootstrapping code that use util-klib and util-io
// by building them, but it's not possible right now, because they use buildSrc plugins (like JPS) and methods (like commonDependency).
// This could be fixed by replacing the buildSrc with the composite build like build-tools, and splitting it to several parts.
//include(":native:kotlin-native-utils")
//include(":kotlin-util-klib")
//include(":kotlin-util-io")

//project(":native:kotlin-native-utils").projectDir = File("$rootDir/../../native/kotlin-native-utils")
//project(":kotlin-util-klib").projectDir = File("$rootDir/../../compiler/util-klib")
//project(":kotlin-util-io").projectDir = File("$rootDir/../../compiler/util-io")

include(":kotlin-native-executors")
project(":kotlin-native-executors").projectDir = File("$rootDir/../../native/executors")