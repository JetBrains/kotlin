// A tool converting kotlinx-benchmark jmh-style json reports into the reports from :benchmarksReports.
plugins {
    // Even though, this does not use kotlin multiplatform, there's a check for validity of
    // `kotlin.native.home` property. During local run, the property is set to a relative path;
    // `custom-kotlin-native-home` will turn it into an absolute one to pass the validation check.
    id("custom-kotlin-native-home")
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":benchmarksReports"))
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
}
