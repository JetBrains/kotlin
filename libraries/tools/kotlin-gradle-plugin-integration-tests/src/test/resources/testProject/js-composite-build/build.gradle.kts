plugins {
    kotlin("js")
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
    implementation("com.example:lib-2")

    implementation(npm("node-fetch", "3.2.8"))
    api(npm("is-odd", "3.0.1"))
    runtimeOnly(npm("is-even", "1.0.0"))
    // No compileOnly dependency because they are not supported. See  IncorrectCompileOnlyDependenciesChecker.

    testImplementation(kotlin("test-js"))
}
