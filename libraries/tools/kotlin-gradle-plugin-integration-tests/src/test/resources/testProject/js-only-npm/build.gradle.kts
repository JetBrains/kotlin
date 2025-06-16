plugins {
    kotlin("multiplatform")
}

group = "com.example"
version = "1.0"

kotlin {
    js {
        binaries.library()
        nodejs()
    }
    sourceSets {
        jsMain {
            dependencies {
                implementation(npm("decamelize", "6.0.0"))
            }
        }
    }
}
