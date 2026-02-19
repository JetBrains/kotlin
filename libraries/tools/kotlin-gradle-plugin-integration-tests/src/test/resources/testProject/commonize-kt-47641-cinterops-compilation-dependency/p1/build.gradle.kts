import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    linuxX64()
    macosX64()
    mingwX64("windowsX64")

    targets.withType<KotlinNativeTarget>().forEach { target ->
        target.compilations.all {
            cinterops.create("withPosix") {
                header(file("libs/withPosix.h"))
            }
        }
    }
}
