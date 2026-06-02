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
    tvosX64("tvosLibX64")

    tvosArm64("tvosLibArm64") {
        println("Configuring ${this.name}")
    }

    tvosX64("tvosLibX64") {
        println("Configuring ${this.name}")
    }

    sourceSets {
        val tvosLibMain = create("tvosLibMain")
        val tvosLibX64Main = getByName("tvosLibX64Main")
        val tvosLibArm64Main = getByName("tvosLibArm64Main")
        tvosLibX64Main.dependsOn(tvosLibMain)
        tvosLibArm64Main.dependsOn(tvosLibMain)
    }
}

publishing {
    repositories {
        maven { setUrl("<localRepo>") }
    }
}
