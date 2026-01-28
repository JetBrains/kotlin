import kotlinx.benchmark.gradle.BenchmarksPlugin
import org.jetbrains.kotlin.benchmarkingTargets

plugins {
    kotlin("multiplatform")
}

kotlin {
    benchmarkingTargets()

    sourceSets.commonMain.dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:${BenchmarksPlugin.PLUGIN_VERSION}")
    }
}