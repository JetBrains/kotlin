import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64()
    linuxArm64()
    // fixme: KT-89587 Clean up tests after iosX64 target deprecation
    iosX64()
    iosArm64()

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
