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
        val watchosLibMain = create("watchosLibMain")
        val watchosLibDeviceMain = create("watchosLibDeviceMain")

        val watchosLibArm32Main = getByName("watchosLibArm32Main")
        val watchosLibArm64Main = getByName("watchosLibArm64Main")
        val watchosLibX64Main = getByName("watchosLibX64Main")

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
