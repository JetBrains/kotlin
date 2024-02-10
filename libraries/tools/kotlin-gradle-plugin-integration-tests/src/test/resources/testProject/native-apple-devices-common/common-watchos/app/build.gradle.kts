plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    watchos()

    // Check that we can reenter the configuration method.
    watchos {
        binaries.framework(listOf(DEBUG))
    }

    sourceSets["watchosMain"].dependencies {
        implementation("common.watchos:lib:1.0")
    }
}
