plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenCentral()
    mavenLocal()
    maven { url = uri("<localRepo>") }
}

kotlin {
    linuxX64 {
        binaries.executable()
    }

    linuxArm64 {
        binaries.executable()
    }

    sourceSets.commonMain.dependencies {
        implementation("org.sample.kt-62515:libb:1.0")
        implementation("org.sample.kt-62515:libc:1.0")
    }
}
