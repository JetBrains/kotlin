group = "com.example"

plugins {
    kotlin("js")
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
    implementation(npm("decamelize", "1.1.1"))
    api(npm("cowsay", "1.6.0"))
    runtimeOnly(npm("uuid", "11.1.0"))
    // No compileOnly dependency because they are not supported. See  IncorrectCompileOnlyDependenciesChecker.
}
