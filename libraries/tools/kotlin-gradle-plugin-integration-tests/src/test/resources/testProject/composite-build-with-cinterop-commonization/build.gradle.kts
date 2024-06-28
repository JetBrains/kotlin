plugins {
    kotlin("multiplatform")
}

group = "org.example"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
    maven("<localRepo>")
}

kotlin {
    applyDefaultHierarchyTemplate()
    jvm()
    linuxX64()
    linuxArm64()

    sourceSets {
        commonMain {
            dependencies {
                api("org.example:included-lib:1.0")
                api(project(":lib"))
            }
        }
    }
}