description = "Runtime library for the Better Web Workers compiler plugin"

plugins {
    kotlin("js")
    `maven-publish`
}

group = "org.jetbrains.kotlin"

repositories {
    mavenCentral()
}

kotlin {
    js {
        browser()
        nodejs()

//        sourceSets {
//            compilations["main"].dependencies {
//                compileOnly(kotlin("stdlib-js"))
//            }
//        }
    }

    sourceSets {
        js().compilations["main"].defaultSourceSet {
            dependencies {
                compileOnly(kotlin("stdlib-js"))
            }
        }
    }
}

configureCommonPublicationSettingsForGradle(signingRequired = false)

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
        }
    }
}