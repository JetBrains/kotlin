plugins {
    kotlin("js") version "<pluginMarkerVersion>"
}

group = "com.example"
version = "1.0"

repositories {
    mavenLocal()
    jcenter()
}

kotlin {
    target {
        browser()
    }

    sourceSets {
        getByName("main") {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }

        getByName("test") {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}