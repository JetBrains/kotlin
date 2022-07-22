plugins {
    id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
    id("maven-publish")
}

group = "common.macos"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    macos("macosLib")
    macos("macosLib") {
        println("Configuring ${this.name}")
    }
}

publishing {
    repositories {
        maven { setUrl(rootProject.projectDir.resolve("repo")) }
    }
}
