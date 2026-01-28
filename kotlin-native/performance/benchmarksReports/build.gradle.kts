import org.jetbrains.kotlin.benchmarkingTargets

plugins {
    id("custom-kotlin-native-home")
    kotlin("multiplatform")
}

kotlin {
    benchmarkingTargets()

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }
    }
}
