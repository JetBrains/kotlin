plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    js {
        nodejs()
        binaries.executable()
        generateTypeScriptDefinitions()
    }
}