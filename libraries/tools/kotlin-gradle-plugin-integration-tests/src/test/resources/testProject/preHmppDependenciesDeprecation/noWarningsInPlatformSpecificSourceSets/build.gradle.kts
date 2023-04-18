plugins {
    kotlin("multiplatform")
    id("maven-publish")
}

publishing {
    repositories {
        maven("<localRepo>")
    }
}

group = "org.jetbrains.kotlin.tests"
version = "0.1"

kotlin {
    jvm()
    js()
    linuxX64()

    listOf("jvmMain", "jsMain", "linuxX64Main").forEach {
        sourceSets.getByName(it).dependencies {
            implementation("org.jetbrains.kotlin.tests:preHmppLibrary:0.1")
        }
    }
}
