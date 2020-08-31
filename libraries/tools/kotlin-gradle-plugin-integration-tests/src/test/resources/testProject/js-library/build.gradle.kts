plugins {
    kotlin("js") version "<pluginMarkerVersion>"
}

group = "com.example"
version = "1.0"

repositories {
    mavenLocal()
    jcenter()
}

kotlin {
    js {
        binaries.library()
        nodejs()
    }
}