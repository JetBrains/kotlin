plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    watchosArm64()
    watchosDeviceArm64()
    watchosX64()
    watchosSimulatorArm64()

    // Check that we can reenter the configuration method.
    watchosArm64 {
        binaries.framework(listOf(DEBUG))
    }

    watchosDeviceArm64 {
        binaries.framework(listOf(DEBUG))
    }

    watchosX64 {
        binaries.framework(listOf(DEBUG))
    }

    watchosSimulatorArm64 {
        binaries.framework(listOf(DEBUG))
    }

    sourceSets {
        val watchosMain by creating
        val watchosDeviceMain by creating
        val watchosTest by creating

        val watchosSimulatorArm64Main by getting
        val watchosDeviceArm64Main by getting
        val watchosArm64Main by getting
        val watchosX64Main by getting

        val watchosSimulatorArm64Test by getting
        val watchosDeviceArm64Test by getting
        val watchosArm64Test by getting
        val watchosX64Test by getting

        watchosDeviceMain.dependsOn(watchosMain)
        watchosX64Main.dependsOn(watchosMain)
        watchosSimulatorArm64Main.dependsOn(watchosMain)

        watchosDeviceArm64Main.dependsOn(watchosDeviceMain)
        watchosArm64Main.dependsOn(watchosDeviceMain)

        watchosSimulatorArm64Test.dependsOn(watchosTest)
        watchosDeviceArm64Test.dependsOn(watchosTest)
        watchosArm64Test.dependsOn(watchosTest)
        watchosX64Test.dependsOn(watchosTest)

        watchosMain.dependencies {
            implementation("common.watchos:lib:1.0")
        }
    }
}
