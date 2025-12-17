group = "com.example"

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin.js {
    nodejs()
    browser()
}

tasks.named("jsBrowserTest") {
    enabled = false
}

kotlin {
    sourceSets {
        jsMain {
            dependencies {
                implementation(npm("decamelize", "1.1.1"))
                api(npm("cowsay", "1.6.0"))
                runtimeOnly(npm("uuid", "11.1.0"))
                // No compileOnly dependency because they are not supported. See  IncorrectCompileOnlyDependenciesChecker.
            }
        }
    }
}
