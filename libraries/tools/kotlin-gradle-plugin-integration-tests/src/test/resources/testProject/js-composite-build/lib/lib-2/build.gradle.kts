plugins {
    kotlin("js")
}

group = "com.example"

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
    implementation("com.example:base2")
    implementation(npm("async", "2.6.2"))
}