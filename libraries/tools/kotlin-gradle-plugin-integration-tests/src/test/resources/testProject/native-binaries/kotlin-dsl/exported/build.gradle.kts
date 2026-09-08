plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    iosArm64("ios")
    @Suppress("DEPRECATION_ERROR") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
    macosX64("macos64")
    linuxX64("linux64")
    mingwX64("mingw64")
    iosX64("iosSim")
}
