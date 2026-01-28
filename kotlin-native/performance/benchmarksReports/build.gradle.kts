import org.jetbrains.kotlin.benchmarkingTargets

plugins {
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