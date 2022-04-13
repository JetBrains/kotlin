plugins {
    kotlin("multiplatform")
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven("<localRepo>")
    }
}

kotlin {
    jvm()
    linuxX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("test:lib:1.0")
            }
        }
    }
}
