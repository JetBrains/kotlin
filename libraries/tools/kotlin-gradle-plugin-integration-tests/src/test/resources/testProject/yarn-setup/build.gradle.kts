import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn

plugins {
    kotlin("js") version "<pluginMarkerVersion>"
}

group = "com.example"
version = "1.0"

repositories {
    mavenLocal()
    jcenter()
}

yarn

kotlin.sourceSets {
    getByName("main") {
        dependencies {
            implementation(kotlin("stdlib-js"))
        }
    }
}

kotlin.target {
    nodejs()
}