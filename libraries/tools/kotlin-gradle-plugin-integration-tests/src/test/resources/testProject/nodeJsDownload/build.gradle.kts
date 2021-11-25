plugins {
    id("org.jetbrains.kotlin.js") version "<pluginMarkerVersion>"
}

repositories {
    mavenLocal()
    mavenCentral()
}


kotlin {
    js {
        nodejs()
    }
}