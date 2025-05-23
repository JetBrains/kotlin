plugins {
    kotlin("multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":lib"))
        }
    }
}
