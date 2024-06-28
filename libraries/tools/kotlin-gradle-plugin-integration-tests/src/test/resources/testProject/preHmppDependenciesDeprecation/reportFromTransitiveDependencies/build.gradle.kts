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
    linuxX64()

    sourceSets.getByName("commonMain").dependencies {
        implementation("org.jetbrains.kotlin.tests:hmppLibraryWithPreHmppInDependencies:0.1")
    }
}
