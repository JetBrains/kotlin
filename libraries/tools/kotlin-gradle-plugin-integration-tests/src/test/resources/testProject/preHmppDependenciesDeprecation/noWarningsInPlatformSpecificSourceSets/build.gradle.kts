plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("<localRepo>")
}

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
