group = "com.example"

plugins {
    kotlin("js") version "<pluginMarkerVersion>"
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin.js {
    nodejs()
    browser()
}

tasks.named("browserTest") {
    enabled = false
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation(npm("decamelize", "1.1.1"))
}