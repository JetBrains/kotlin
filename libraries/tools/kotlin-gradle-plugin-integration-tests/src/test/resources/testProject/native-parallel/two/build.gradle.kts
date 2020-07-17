plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    linuxX64("linux")

    sourceSets["commonMain"].dependencies {
        implementation(kotlin("stdlib"))
    }
}