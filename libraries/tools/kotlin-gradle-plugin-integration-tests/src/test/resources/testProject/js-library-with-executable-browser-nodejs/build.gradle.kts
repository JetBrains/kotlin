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
        // For now we do not recommend multiple binaries, so we test it just in case
        binaries.library()
        binaries.executable()
        nodejs()
        browser()
    }
}

// We need it for suppress warnings of Gradle 7.0
// We need to think about it, when we will support multiple binaries
tasks.named("jsNodeProductionLibraryDistribution") {
    mustRunAfter("productionExecutableCompileSync")
}

tasks.named("browserProductionLibraryDistribution") {
    mustRunAfter("productionExecutableCompileSync")
}

tasks.named("jsBrowserProductionWebpack") {
    mustRunAfter("productionLibraryCompileSync")
}