plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    jvm()
    js(IR) {
        nodejs()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("included-build:included")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
            }
        }
    }
}
