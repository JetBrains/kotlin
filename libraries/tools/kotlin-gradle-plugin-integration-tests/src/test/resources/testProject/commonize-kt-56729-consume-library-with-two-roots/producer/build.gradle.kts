@file:Suppress("OPT_IN_USAGE")

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    `maven-publish`
}

version = "1.0.0-SNAPSHOT"
group = "org.jetbrains.sample"

publishing {
    repositories {
        maven(buildDir.resolve("repo"))
    }
}

kotlin {
    linuxX64()
    linuxArm64()

    targetHierarchy.custom {
        group("nativeCommon") {
            withNative()
        }
    }

    targets.withType<KotlinNativeTarget>().all {
        compilations.getByName("main").cinterops.create("producer-cinterop-library") {
            header("src/producer-c-library.h")
        }
    }
}