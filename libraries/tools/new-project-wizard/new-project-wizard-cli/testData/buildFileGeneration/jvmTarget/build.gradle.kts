plugins {
    kotlin("multiplatform") version "KOTLIN_VERSION"
}

group = "testGroupId"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("KOTLIN_REPO")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    sourceSets {
        val jvmMain by getting
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}