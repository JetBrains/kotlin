plugins {
    kotlin("js").version("<pluginMarkerVersion>")
}

group = "com.example"
version = "1.0"

allprojects {
    repositories {
        mavenLocal()
        jcenter()
    }
}