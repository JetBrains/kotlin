plugins {
    kotlin("js")
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation(project(":lib"))
}

kotlin {
    target {
        browser {
        }
        produceExecutable()
    }
}