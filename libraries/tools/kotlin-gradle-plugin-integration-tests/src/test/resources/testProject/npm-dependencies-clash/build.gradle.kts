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
        }
    }

    getByName("test") {
        dependencies {
            implementation(kotlin("test-js"))
            implementation(npm("mocha"))
            implementation(npm("puppeteer"))
        }
    }
}

kotlin.target {
    nodejs()
}