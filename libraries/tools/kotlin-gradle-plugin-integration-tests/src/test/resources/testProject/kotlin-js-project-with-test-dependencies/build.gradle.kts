plugins {
    kotlin("js").version("<pluginMarkerVersion>")
}

group = "com.example"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    js(IR) {
        nodejs {
        }
    }
}

dependencies {
    testRuntimeOnly(npm("xmlhttprequest", "1.8.0"))
}