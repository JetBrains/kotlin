plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    iosX64()

    // Check that we can reenter the configuration method.
    iosX64 {
        binaries.framework(listOf(DEBUG))
    }

    sourceSets["iosMain"].dependencies {
        implementation("common.ios:lib:1.0")
    }
}
