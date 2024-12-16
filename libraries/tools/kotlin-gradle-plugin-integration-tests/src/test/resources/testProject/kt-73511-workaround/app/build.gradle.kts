plugins {
    kotlin("multiplatform")
}

group = "org.sample.kt-73511"
version = 1.0

repositories {
    mavenCentral()
    maven { url = uri("<localRepo>") }
}

kotlin {
    macosArm64 {
        binaries {
            executable()
        }
    }

    sourceSets.commonMain.dependencies {
        implementation("org.sample.kt-73511:lib1:1.0")
        implementation("org.sample.kt-73511:lib2:1.0")
    }
}
