plugins {
    kotlin("multiplatform")
}

group = "org.jetbrains.qa"
version = "1.0-SNAPSHOT"

kotlin {
    linuxX64()
    mingwX64() // can be any dummy target here, just for sake of nativeMain auto-insertion
}
