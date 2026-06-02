plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()
    js {
        nodejs()
    }

    sourceSets {
        val commonMain = getByName("commonMain") {
            dependencies {
                implementation("included-build:included")
            }
        }
        val commonTest = getByName("commonTest") {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
            }
        }
    }
}

