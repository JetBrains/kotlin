description = "Runtime library for the Atomicfu compiler plugin"

plugins {
    kotlin("js")
}

group = "org.jetbrains.kotlin"

repositories {
    mavenCentral()
}

kotlin {
    js()

    sourceSets {
        js().compilations["main"].defaultSourceSet {
            dependencies {
                compileOnly(kotlin("stdlib-js"))
            }
        }
    }
}