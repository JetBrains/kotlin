plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    iosArm64("ios")
    macosArm64("macos64")
    linuxX64("linux64")
    linuxArm64("linuxArm64")
    mingwX64("mingw64")
    iosX64("iosSim")
}
