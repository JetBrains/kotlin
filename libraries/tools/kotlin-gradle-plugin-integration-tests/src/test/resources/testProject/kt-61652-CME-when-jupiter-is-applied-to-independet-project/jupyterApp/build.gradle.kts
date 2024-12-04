plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.jupyter.api") version "0.12.0-62"
}

kotlin {
    jvm()
    linuxX64()
}