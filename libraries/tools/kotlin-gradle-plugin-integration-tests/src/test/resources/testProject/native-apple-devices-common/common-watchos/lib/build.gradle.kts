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
        val watchosLibMain = create("watchosLibMain")
        val watchosLibDeviceMain = create("watchosLibDeviceMain")

        val watchosLibDeviceArm64Main = getByName("watchosLibDeviceArm64Main")
        val watchosLibSimulatorArm64Main = getByName("watchosLibSimulatorArm64Main")
        val watchosLibArm64Main = getByName("watchosLibArm64Main")
        val watchosLibX64Main = getByName("watchosLibX64Main")

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
