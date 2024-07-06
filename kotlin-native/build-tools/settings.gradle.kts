rootProject.name = "native-build-tools"

pluginManagement {
    apply(from = "../../repo/scripts/cache-redirector.settings.gradle.kts")
    apply(from = "../../repo/scripts/kotlin-bootstrap.settings.gradle.kts")

    includeBuild("../../repo/gradle-settings-conventions")
//    includeBuild("../../repo/gradle-build-conventions")

    repositories {
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
        google()
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