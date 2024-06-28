import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js().nodejs()

    linuxX64()
    macosX64()
    mingwX64("windowsX64")

    val commonMain by sourceSets.getting

    commonMain.dependencies {
        implementation(project(":p1"))
        implementation(project(":p0"))
    }

    targets.withType<KotlinNativeTarget>().forEach { target ->
        target.compilations.all {
            cinterops.create("withPosix") {
                header(file("libs/withPosix.h"))
            }

            cinterops.create("withPosixP2") {
                header(file("libs/withPosixP2.h"))
            }
        }
    }
}
