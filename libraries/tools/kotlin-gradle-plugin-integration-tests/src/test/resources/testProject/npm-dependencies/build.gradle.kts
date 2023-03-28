plugins {
    kotlin("js")
}

group = "com.example"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin.sourceSets {
    getByName("main") {
        dependencies {
            implementation(kotlin("stdlib-js"))
            // It has transitive dependency on custom github version of escodegen
            implementation(npm("@yworks/optimizer", "^1.0.5"))
            implementation(npm("@yworks/optimizer", "1.0.6"))
            implementation(npm("file-dependency", projectDir.resolve("file-dependency")))
            implementation(npm(projectDir.resolve("file-dependency-2")))
            implementation(npm(projectDir.resolve("file-dependency-3")))
            implementation(devNpm("42", "0.0.1"))
            implementation(npm("react", " >= 16.4.0 < 16.9.0"))
            implementation(peerNpm("date-arithmetic", "4.1.0"))
        }
    }

    getByName("test") {
        dependencies {
            implementation(kotlin("test-js"))
            implementation(npm("mocha", "*"))
        }
    }
}

kotlin.target {
    nodejs()
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile> {
    kotlinOptions.freeCompilerArgs += "-Xforce-deprecated-legacy-compiler-usage"
}
