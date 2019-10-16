plugins {
    id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
    id("maven-publish")
}

group = "common.ios"
version = "1.0"

repositories {
    mavenLocal()
    jcenter()
}

kotlin {
    ios("iosLib")
    ios("iosLib") {
        println("Configuring ${this.name}")
    }
}

publishing {
    repositories {
        maven { setUrl(rootProject.projectDir.resolve("repo")) }
    }
}
