plugins {
    kotlin("js")
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
        main {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.3")
                implementation(npm("kotlin", "*"))
            }
        }
    }
}
