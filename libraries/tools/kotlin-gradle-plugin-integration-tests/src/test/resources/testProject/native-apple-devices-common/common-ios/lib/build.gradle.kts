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
    iosX64("iosLibX64")
    iosArm64("iosLibArm64")

    iosX64("iosLibX64") {
        println("Configuring ${this.name}")
    }

    iosArm64("iosLibArm64") {
        println("Configuring ${this.name}")
    }

    sourceSets {
        val iosLibMain by creating
        val iosLibX64Main by getting
        val iosLibArm64Main by getting
        iosLibX64Main.dependsOn(iosLibMain)
        iosLibArm64Main.dependsOn(iosLibMain)
    }
}

publishing {
    repositories {
        maven { setUrl("<localRepo>") }
    }
}
