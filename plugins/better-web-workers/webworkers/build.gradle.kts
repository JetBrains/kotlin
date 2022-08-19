import plugins.signLibraryPublication

description = "Runtime library for the WebWorkers compiler plugin"

plugins {
    kotlin("js")
    `maven-publish`
}

group = "kotlinx.webworkers"

repositories {
    mavenCentral()
}

kotlin {
    js(IR) {
        browser()
        nodejs()
        binaries.library()
    }

    sourceSets {
        js(IR).compilations["main"].defaultSourceSet {
            dependencies {
                compileOnly(kotlin("stdlib-js"))
            }
        }
    }
}

configureCommonPublicationSettingsForGradle(signLibraryPublication)

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
        }
    }
}