// A tool converting kotlinx-benchmark jmh-style json reports into the reports from :benchmarksReports.
plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":benchmarksReports"))
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
}
