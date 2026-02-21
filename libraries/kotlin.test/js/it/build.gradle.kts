plugins {
    id("root-config")
    kotlin("multiplatform")
}

description = "Kotlin-test integration tests for JS"

kotlin {
    js {
    }

    sourceSets {
        val jsMain by getting {
            kotlin.srcDir("src/main/kotlin")
            dependencies {
                implementation(project(":kotlin-stdlib"))
            }
        }
        val jsTest by getting {
            kotlin.srcDir("src/test/kotlin")
            dependencies {
                implementation(project(":kotlin-test"))
            }
        }
    }
}
