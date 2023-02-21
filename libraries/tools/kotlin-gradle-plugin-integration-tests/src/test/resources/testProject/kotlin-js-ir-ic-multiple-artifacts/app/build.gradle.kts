plugins {
    kotlin("js")
}

dependencies {
    implementation(project(":lib"))
    testImplementation(kotlin("test-js"))
}

kotlin {
    js {
        browser {
        }
        binaries.executable()
    }
}
