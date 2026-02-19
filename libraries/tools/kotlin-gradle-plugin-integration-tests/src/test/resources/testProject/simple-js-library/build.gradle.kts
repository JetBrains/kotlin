plugins {
    kotlin("multiplatform")
}

group = "com.example"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    js {
        binaries.library()
        nodejs()
    }
    sourceSets {
        jsMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.3")
                implementation(npm("decamelize", "6.0.0"))
            }
        }
    }
}
