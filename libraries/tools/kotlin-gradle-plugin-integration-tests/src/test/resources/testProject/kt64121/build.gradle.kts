plugins {
    kotlin("multiplatform")
    `maven-publish`
}

repositories {
    mavenLocal()
    maven("../repo")
    mavenCentral()
}

group = "com.example.bar"
version = "1.0"

kotlin {
    js()
    linuxX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }

        val linuxAndJsMain by creating {
            dependsOn(commonMain)
        }

        js().compilations["main"].defaultSourceSet {
            dependsOn(linuxAndJsMain)
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }

        linuxX64().compilations["main"].defaultSourceSet {
            dependsOn(linuxAndJsMain)
        }
    }
}

publishing {
    repositories {
        maven("../repo")
    }
}
