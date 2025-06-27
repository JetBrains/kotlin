plugins {
    kotlin("js") version "<pluginMarkerVersion>"
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    js {
        nodejs()
        binaries.executable()
    }
}
kotlin {
    js {
        browser()
        binaries.executable()
    }
}

tasks.named("browserTest") {
    enabled = false
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation("com.example:lib-2")
    implementation(npm("node-fetch", "3.2.8"))
    testImplementation(kotlin("test-js"))
}