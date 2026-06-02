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
        val watchosMain = create("watchosMain")
        val watchosDeviceMain = create("watchosDeviceMain")
        val watchosTest = create("watchosTest")

        val watchosArm32Main = getByName("watchosArm32Main")
        val watchosArm64Main = getByName("watchosArm64Main")
        val watchosX64Main = getByName("watchosX64Main")

        val watchosArm32Test = getByName("watchosArm32Test")
        val watchosArm64Test = getByName("watchosArm64Test")
        val watchosX64Test = getByName("watchosX64Test")

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
