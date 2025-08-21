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
    implementation("com.example:base2")

    implementation(npm("tiny-invariant", "1.3.3"))
    api(npm("is-obj", "3.0.0"))
    runtimeOnly(npm("async", "2.6.2"))
    // No compileOnly dependency because they are not supported. See  IncorrectCompileOnlyDependenciesChecker.
}
