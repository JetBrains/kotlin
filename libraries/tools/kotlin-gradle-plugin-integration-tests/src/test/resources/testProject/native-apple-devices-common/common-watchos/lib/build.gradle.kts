plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("maven-publish")
}

group = "common.watchos"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    watchosArm32("watchosLibArm32")
    watchosArm64("watchosLibArm64")
    watchosX64("watchosLibX64")

    watchosArm32("watchosLibArm32") {
        println("Configuring ${this.name}")
    }

    watchosArm64("watchosLibArm64") {
        println("Configuring ${this.name}")
    }

    watchosX64("watchosLibX64") {
        println("Configuring ${this.name}")
    }

    sourceSets {
        val watchosLibMain by creating
        val watchosLibDeviceMain by creating

        val watchosLibArm32Main by getting
        val watchosLibArm64Main by getting
        val watchosLibX64Main by getting

        watchosLibDeviceMain.dependsOn(watchosLibMain)
        watchosLibX64Main.dependsOn(watchosLibMain)

        watchosLibArm32Main.dependsOn(watchosLibDeviceMain)
        watchosLibArm64Main.dependsOn(watchosLibDeviceMain)
    }
}

publishing {
    repositories {
        maven { setUrl("<localRepo>") }
    }
}
