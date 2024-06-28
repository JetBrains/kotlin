import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
}

kotlin {
    macosX64()
    macosArm64()
    linuxArm64()
    linuxX64()
    /*
    https://youtrack.jetbrains.com/issue/KT-63721/Commonizer-Uncovered-platform.posix.socket-not-commonized-for-mingw-linux-macos
    Not building for windows, because it would require writing expect/actuals around
    signed vs unsigned socket APIs:
    mingwX64()
     */

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries {
            executable {
                entryPoint = "sample.echoserver.main"
                runTask?.args(3000)
            }
        }
    }
}
