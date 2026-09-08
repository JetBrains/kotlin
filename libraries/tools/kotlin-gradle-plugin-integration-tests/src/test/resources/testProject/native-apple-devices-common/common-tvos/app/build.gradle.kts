plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    tvosArm64()
    @Suppress("DEPRECATION_ERROR") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
    tvosX64()

    // Check that we can reenter the configuration method.
    tvosArm64 {
        binaries.framework(listOf(DEBUG))
    }

    @Suppress("DEPRECATION_ERROR") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
    tvosX64 {
        binaries.framework(listOf(DEBUG))
    }

    sourceSets.tvosMain.dependencies {
        implementation("common.tvos:lib:1.0")
    }
}
