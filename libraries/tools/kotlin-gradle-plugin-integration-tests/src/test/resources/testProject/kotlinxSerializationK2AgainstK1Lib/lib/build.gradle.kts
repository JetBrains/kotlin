plugins {
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    `maven-publish`
}

repositories {
    mavenLocal()
    mavenCentral()
}

group = "com.example.serialization_lib"
version = "1.0"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
}

publishing {
    repositories {
        maven("<localRepo>")
    }

    publications {
        create<MavenPublication>("Maven") {
            from(components["java"])
        }
    }
}
