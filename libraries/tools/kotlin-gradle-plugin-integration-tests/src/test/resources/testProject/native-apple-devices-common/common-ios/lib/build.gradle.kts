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
        val iosLibMain = create("iosLibMain")
        val iosLibX64Main = getByName("iosLibX64Main")
        val iosLibArm64Main = getByName("iosLibArm64Main")
        iosLibX64Main.dependsOn(iosLibMain)
        iosLibArm64Main.dependsOn(iosLibMain)
    }
}

publishing {
    repositories {
        maven { setUrl("<localRepo>") }
    }
}
