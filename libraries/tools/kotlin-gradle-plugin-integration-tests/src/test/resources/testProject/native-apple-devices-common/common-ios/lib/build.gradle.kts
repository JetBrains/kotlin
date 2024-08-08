plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("maven-publish")
}

group = "common.ios"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    iosX64("iosLib")
    iosX64("iosLib") {
        println("Configuring ${this.name}")
    }
}

publishing {
    repositories {
        maven { setUrl("<localRepo>") }
    }
}
