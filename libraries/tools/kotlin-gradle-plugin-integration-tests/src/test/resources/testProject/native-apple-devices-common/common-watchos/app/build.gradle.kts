plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    watchosArm32()
    watchosArm64()
    watchosX64()

    // Check that we can reenter the configuration method.
    watchosArm32 {
        binaries.framework(listOf(DEBUG))
    }

    watchosArm64 {
        binaries.framework(listOf(DEBUG))
    }

    watchosX64 {
        binaries.framework(listOf(DEBUG))
    }

    sourceSets {
        val watchosMain by creating
        val watchosDeviceMain by creating
        val watchosTest by creating

        val watchosArm32Main by getting
        val watchosArm64Main by getting
        val watchosX64Main by getting

        val watchosArm32Test by getting
        val watchosArm64Test by getting
        val watchosX64Test by getting

        watchosDeviceMain.dependsOn(watchosMain)
        watchosX64Main.dependsOn(watchosMain)

        watchosArm32Main.dependsOn(watchosDeviceMain)
        watchosArm64Main.dependsOn(watchosDeviceMain)

        watchosArm32Test.dependsOn(watchosTest)
        watchosArm64Test.dependsOn(watchosTest)
        watchosX64Test.dependsOn(watchosTest)

        watchosMain.dependencies {
            implementation("common.watchos:lib:1.0")
        }
    }
}
