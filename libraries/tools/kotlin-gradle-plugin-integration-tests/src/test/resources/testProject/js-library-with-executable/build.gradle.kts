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
        // For now we do not recommend multiple binaries, so we test it just in case
        binaries.library()
        binaries.executable()
        nodejs()
    }
}

// We need it for suppress warnings of Gradle 7.0
// We need to think about it, when we will support multiple binaries
tasks.named("nodeProductionLibraryDistribution") {
    mustRunAfter("productionExecutableCompileSync")
}
