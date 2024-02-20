plugins {
    kotlin("multiplatform")
}

group = "komem.litmus"
version = "1.0-SNAPSHOT"

kotlin {
    jvm()
    sourceSets {
        jvmMain {
            dependencies {
                implementation("com.google.devtools.ksp:symbol-processing-api:1.9.10-1.0.13")
            }
            kotlin.srcDir("src/main/kotlin")
            resources.srcDir("src/main/resources")
        }
    }
}
