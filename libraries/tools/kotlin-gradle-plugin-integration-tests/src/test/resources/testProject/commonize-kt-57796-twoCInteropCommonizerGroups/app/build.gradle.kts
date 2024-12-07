import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64()
    linuxArm64()
    macosX64()
    macosArm64()

    applyDefaultHierarchyTemplate()

    targets.withType<KotlinNativeTarget>().all {
        compilations.getByName("main").cinterops.create("libapp") {
            headers(file("libapp.h"))
        }
    }

    sourceSets.commonMain.get().dependencies {
        implementation(project(":lib"))
    }
}