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
        val commonMain = getByName("commonMain") {
            dependencies {
                implementation(project(":module2"))
            }
        }
    }
}