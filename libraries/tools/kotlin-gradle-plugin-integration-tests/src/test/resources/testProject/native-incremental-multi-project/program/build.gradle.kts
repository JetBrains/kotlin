plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    <SingleNativeTarget>("host") {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":library"))
            }
        }
    }
}
