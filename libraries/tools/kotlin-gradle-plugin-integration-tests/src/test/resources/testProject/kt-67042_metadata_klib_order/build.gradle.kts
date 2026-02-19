plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js().browser()

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.ui:ui:1.6.0")
        }
    }
}
