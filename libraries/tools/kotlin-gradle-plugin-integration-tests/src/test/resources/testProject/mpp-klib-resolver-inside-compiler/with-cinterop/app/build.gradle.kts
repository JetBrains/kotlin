plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

val repo: File = file("<localRepo>")

repositories {
    mavenCentral()
    mavenLocal()
    maven { url = uri(repo) }
}

kotlin {
    macosArm64 {
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
