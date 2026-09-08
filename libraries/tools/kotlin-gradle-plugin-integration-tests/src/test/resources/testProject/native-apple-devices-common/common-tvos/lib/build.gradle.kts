plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("maven-publish")
}

group = "common.tvos"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    tvosArm64("tvosLibArm64")
    tvosSimulatorArm64("tvosLibSimulatorArm64")

    tvosArm64("tvosLibArm64") {
        logger.lifecycle("Configuring ${this.name}")
    }

    tvosSimulatorArm64("tvosLibSimulatorArm64") {
        logger.lifecycle("Configuring ${this.name}")
    }

    sourceSets {
        val tvosLibMain = create("tvosLibMain")
        val tvosLibSimulatorArm64Main = getByName("tvosLibSimulatorArm64Main")
        val tvosLibArm64Main = getByName("tvosLibArm64Main")
        tvosLibSimulatorArm64Main.dependsOn(tvosLibMain)
        tvosLibArm64Main.dependsOn(tvosLibMain)
    }
}

publishing {
    repositories {
        maven { setUrl("<localRepo>") }
    }
}
