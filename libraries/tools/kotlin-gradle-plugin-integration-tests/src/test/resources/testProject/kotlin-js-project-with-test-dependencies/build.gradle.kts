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
        nodejs {
        }
    }
}

dependencies {
    testRuntimeOnly(npm("xmlhttprequest", "1.8.0"))
}