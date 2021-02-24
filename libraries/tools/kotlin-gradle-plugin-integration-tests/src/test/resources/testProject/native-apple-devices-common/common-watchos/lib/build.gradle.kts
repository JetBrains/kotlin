plugins {
    id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
    id("maven-publish")
}

group = "common.watchos"
version = "1.0"

repositories {
    mavenLocal()
    jcenter()
}

kotlin {
    watchos("watchosLib")
    watchos("watchosLib") {
        println("Configuring ${this.name}")
    }
}

publishing {
    repositories {
        maven { setUrl(rootProject.projectDir.resolve("repo")) }
    }
}
