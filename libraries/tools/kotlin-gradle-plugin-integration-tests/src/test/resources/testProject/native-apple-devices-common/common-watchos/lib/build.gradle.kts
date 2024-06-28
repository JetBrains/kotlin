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
    watchos("watchosLib")
    watchos("watchosLib") {
        println("Configuring ${this.name}")
    }
}

publishing {
    repositories {
        maven { setUrl("<localRepo>") }
    }
}
