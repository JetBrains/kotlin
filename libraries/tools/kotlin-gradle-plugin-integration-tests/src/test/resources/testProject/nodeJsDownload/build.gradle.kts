plugins {
    id("org.jetbrains.kotlin.js")
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