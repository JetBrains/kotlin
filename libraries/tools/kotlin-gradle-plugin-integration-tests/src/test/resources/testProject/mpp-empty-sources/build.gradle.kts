plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    jvmToolchain(8)
    jvm {
        withJava()
    }
    js(IR) {
        binaries.executable()
        browser {
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
        }
    }
    repositories {
        maven("<localRepo>")
    }
}
