plugins {
    kotlin("multiplatform") version "<pluginMarkerVersion>"
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven("${rootProject.projectDir}/../repo")
    }
}

kotlin {
    linuxX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("test:lib:1.0")
            }
        }
    }
}
