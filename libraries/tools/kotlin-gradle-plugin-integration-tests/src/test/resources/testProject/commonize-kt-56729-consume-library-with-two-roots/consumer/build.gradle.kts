@file:Suppress("OPT_IN_USAGE")

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxArm64()
    linuxX64()

    applyDefaultHierarchyTemplate()

    sourceSets.commonMain.get().dependencies {
        implementation("org.jetbrains.sample:producer:1.0.0-SNAPSHOT")
    }

    targets.withType<KotlinNativeTarget>().all {
        compilations.getByName("main").cinterops.create("consumer-c-library") {
            header("src/consumer-c-library.h")
        }
    }
}
