import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64()
    linuxArm64()
    @Suppress("DEPRECATION_ERROR") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
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