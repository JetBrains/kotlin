import plugins.signLibraryPublication

description = "Runtime library for the Atomicfu compiler plugin"

plugins {
    kotlin("js")
    `maven-publish`
}

group = "org.jetbrains.kotlin"

repositories {
    mavenCentral()
}

kotlin {
    js() {
        browser()
        nodejs()
    }

    sourceSets {
        js().compilations["main"].defaultSourceSet {
            dependencies {
                compileOnly(kotlin("stdlib-js"))
            }
        }
    }
}

suppressYarnAndNpmForAssemble()

configureCommonPublicationSettingsForGradle(signLibraryPublication)

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
        }
    }
}
