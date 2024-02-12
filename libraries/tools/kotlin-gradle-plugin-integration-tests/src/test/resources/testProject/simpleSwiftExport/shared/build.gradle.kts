plugins {
    kotlin("multiplatform")
}

kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "sdk"
        }
    }

    sourceSets {
        val commonMain by getting
    }
}
