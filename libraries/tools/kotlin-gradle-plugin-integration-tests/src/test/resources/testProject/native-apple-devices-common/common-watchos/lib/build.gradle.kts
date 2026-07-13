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
    watchosDeviceArm64("watchosLibDeviceArm64")
    watchosSimulatorArm64("watchosLibSimulatorArm64")
    watchosArm64("watchosLibArm64")
    watchosX64("watchosLibX64")

    watchosDeviceArm64("watchosLibDeviceArm64") {
        logger.lifecycle("Configuring ${this.name}")
    }

    watchosSimulatorArm64("watchosLibSimulatorArm64") {
        logger.lifecycle("Configuring ${this.name}")
    }

    watchosArm64("watchosLibArm64") {
        logger.lifecycle("Configuring ${this.name}")
    }

    watchosX64("watchosLibX64") {
        logger.lifecycle("Configuring ${this.name}")
    }

    sourceSets {
        val watchosLibMain by creating
        val watchosLibDeviceMain by creating

        val watchosLibDeviceArm64Main by getting
        val watchosLibSimulatorArm64Main by getting
        val watchosLibArm64Main by getting
        val watchosLibX64Main by getting

        watchosLibDeviceMain.dependsOn(watchosLibMain)
        watchosLibX64Main.dependsOn(watchosLibMain)
        watchosLibSimulatorArm64Main.dependsOn(watchosLibMain)

        watchosLibDeviceArm64Main.dependsOn(watchosLibDeviceMain)
        watchosLibArm64Main.dependsOn(watchosLibDeviceMain)
    }
}

publishing {
    repositories {
        maven { setUrl("<localRepo>") }
    }
}
