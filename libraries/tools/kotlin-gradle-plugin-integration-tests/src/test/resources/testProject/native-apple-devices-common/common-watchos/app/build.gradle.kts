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
        val watchosMain = create("watchosMain")
        val watchosDeviceMain = create("watchosDeviceMain")
        val watchosTest = create("watchosTest")

        val watchosSimulatorArm64Main = getByName("watchosSimulatorArm64Main")
        val watchosDeviceArm64Main = getByName("watchosDeviceArm64Main")
        val watchosArm64Main = getByName("watchosArm64Main")
        val watchosX64Main = getByName("watchosX64Main")

        val watchosSimulatorArm64Test = getByName("watchosSimulatorArm64Test")
        val watchosDeviceArm64Test = getByName("watchosDeviceArm64Test")
        val watchosArm64Test = getByName("watchosArm64Test")
        val watchosX64Test = getByName("watchosX64Test")

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
