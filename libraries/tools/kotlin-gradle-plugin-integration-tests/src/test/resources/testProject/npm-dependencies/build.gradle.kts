plugins {
    kotlin("js") version "<pluginMarkerVersion>"
}

group = "com.example"
version = "1.0"

repositories {
    mavenLocal()
    jcenter()
}

kotlin.sourceSets {
    getByName("main") {
        dependencies {
            implementation(kotlin("stdlib-js"))
            // It has transitive dependency on custom github version of escodegen
            implementation(npm("@yworks/optimizer", "1.0.6"))
            implementation(npm("file-dependency", projectDir.resolve("file-dependency")))
            implementation(npm(projectDir.resolve("file-dependency-2")))
            implementation(npm(projectDir.resolve("file-dependency-3")))
        }
    }

    getByName("test") {
        dependencies {
            implementation(kotlin("test-js"))
            implementation(npm("mocha"))
        }
    }
}

kotlin.target {
    nodejs()
}