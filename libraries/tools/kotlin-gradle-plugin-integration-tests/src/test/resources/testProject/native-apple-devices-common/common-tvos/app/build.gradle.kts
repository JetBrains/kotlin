plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    tvosArm64()
    tvosSimulatorArm64()

    // Check that we can reenter the configuration method.
    tvosArm64 {
        binaries.framework(listOf(DEBUG))
    }

    tvosSimulatorArm64 {
        binaries.framework(listOf(DEBUG))
    }

    sourceSets.tvosMain.dependencies {
        implementation("common.tvos:lib:1.0")
    }
}
