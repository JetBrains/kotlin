plugins {
    kotlin("multiplatform") version "<pluginMarkerVersion>"
    `maven-publish`
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    jvm {
        jvmToolchain(8)
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
        maven { setUrl("${rootProject.buildDir}/repo") }
    }
}