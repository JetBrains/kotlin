plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "test"
version = "1.0"

repositories {
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
                api(project(":libWithCustomLayout"))
            }
        }
    }
}

publishing {
    repositories {
        maven("<localRepo>")
    }
}