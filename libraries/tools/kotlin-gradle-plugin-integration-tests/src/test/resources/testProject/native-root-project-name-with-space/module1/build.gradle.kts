plugins {
    kotlin("multiplatform")
}

kotlin {
    <SingleNativeTarget>("host") {
        binaries {
            sharedLib {
                baseName = "shared"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":module2"))
            }
        }
    }
}