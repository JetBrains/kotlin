@file:Suppress("OPT_IN_USAGE")

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    `maven-publish`
}


group = "org.jetbrains.sample"
version = "1.0.0-SNAPSHOT"

kotlin {
    applyDefaultHierarchyTemplate()
    jvm()
    linuxX64()
    linuxArm64()

    targets.withType<KotlinNativeTarget>().all {
        compilations.getByName("main").cinterops.create("clib") {
            header("src/clib.h")
        }
    }
}
