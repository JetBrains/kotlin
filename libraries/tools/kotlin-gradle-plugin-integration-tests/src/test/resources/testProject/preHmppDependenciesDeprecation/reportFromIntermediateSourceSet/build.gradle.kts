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
    js()

    sourceSets {
        val commonMain by getting

        val intermediate by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("org.jetbrains.kotlin.tests:preHmppLibrary:0.1")
            }
        }

        val jvmMain by getting {
            dependsOn(intermediate)
        }

        val linuxX64Main by getting {
            dependsOn(intermediate)
        }
    }
}
