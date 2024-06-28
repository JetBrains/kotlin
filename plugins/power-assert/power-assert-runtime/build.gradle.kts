import plugins.configureDefaultPublishing

description = "Runtime library for the Power-Assert compiler plugin"

plugins {
    kotlin("multiplatform")
    `maven-publish`
    signing
}

kotlin {
    explicitApi()

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlinStdlib())
            }
        }
    }
}

configureDefaultPublishing()
