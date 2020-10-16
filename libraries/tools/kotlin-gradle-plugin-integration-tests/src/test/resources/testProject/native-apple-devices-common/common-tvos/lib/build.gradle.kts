plugins {
    id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
    id("maven-publish")
}

group = "common.tvos"
version = "1.0"

repositories {
    mavenLocal()
    jcenter()
}

kotlin {
    tvos("tvosLib")
    tvos("tvosLib") {
        println("Configuring ${this.name}")
    }
}

publishing {
    repositories {
        maven { setUrl(rootProject.projectDir.resolve("repo")) }
    }
}
