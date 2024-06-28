plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64 {
        binaries {
            executable()
        }
    }
    sourceSets.commonMain.get().dependencies {
        implementation("org.jetbrains.sample:p2:1.0.0")
    }
}
