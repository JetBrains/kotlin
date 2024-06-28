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
    ios("iosLib")
    ios("iosLib") {
        println("Configuring ${this.name}")
    }
}

publishing {
    repositories {
        maven { setUrl("<localRepo>") }
    }
}
