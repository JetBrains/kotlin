import org.jetbrains.kotlin.benchmarkingTargets

plugins {
    id("custom-kotlin-native-home")
    kotlin("multiplatform")
}

kotlin {
    benchmarkingTargets()
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }
    }
}
