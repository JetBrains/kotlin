plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    tvos()

    // Check that we can reenter the configuration method.
    tvos {
        binaries.framework(listOf(DEBUG))
    }

    sourceSets["tvosMain"].dependencies {
        implementation("common.tvos:lib:1.0")
    }
}
