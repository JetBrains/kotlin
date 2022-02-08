plugins {
    kotlin("multiplatform").version("<pluginMarkerVersion>")
    `maven-publish`
}

repositories {
    mavenLocal()
    maven("../repo")
    mavenCentral()
}

group = "com.example.foo"
version = "1.0"

kotlin {
    js()

    sourceSets {
        js().compilations["main"].defaultSourceSet {
            dependencies {
                api("com.example.thirdparty:third-party-lib:1.0")
                implementation(kotlin("stdlib-js"))
            }
        }

        js().compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

publishing {
    repositories {
        maven("../repo")
    }
}