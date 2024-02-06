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
    tvos("tvosLib")
    tvos("tvosLib") {
        println("Configuring ${this.name}")
    }
}

publishing {
    repositories {
        maven { setUrl("<localRepo>") }
    }
}
