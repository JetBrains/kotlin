rootProject.name = "native-build-tools"

pluginManagement {
    includeBuild("../../repo/gradle-settings-conventions")

    repositories {
        maven("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
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

buildscript {
    val buildGradlePluginVersion = extra["kotlin.build.gradlePlugin.version"]
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-build-gradle-plugin:$buildGradlePluginVersion")
    }
}