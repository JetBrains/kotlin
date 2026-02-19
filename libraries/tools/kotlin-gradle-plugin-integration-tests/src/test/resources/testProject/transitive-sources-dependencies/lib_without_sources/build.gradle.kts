plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "test"
version = "1.0"

publishing {
    repositories {
        maven("<localRepo>")
    }
}

kotlin {
    withSourcesJar(publish = false)
    jvm()
    linuxX64()

    sourceSets {
        commonMain.dependencies {
            api(project(":lib_with_sources"))
        }
    }
}