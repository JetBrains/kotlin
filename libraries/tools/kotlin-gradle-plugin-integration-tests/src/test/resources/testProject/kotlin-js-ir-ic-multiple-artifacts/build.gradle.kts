plugins {
    kotlin("js").version("<pluginMarkerVersion>").apply(false)
}

group = "com.example"
version = "1.0"

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}