plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    linuxArm32Hfp("intPlatform") {
        compilations.get("main").cinterops.create("intPropertyInterop") {
            header(file("libs/intPlatform.h"))
        }
    }
    linuxX64("longPlatform") {
        compilations.get("main").cinterops.create("intPropertyInterop") {
            header(file("libs/longPlatform.h"))
        }
    }
}
