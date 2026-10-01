plugins {
    `kotlin-dsl`
    kotlin("jvm")
}

sourceSets["main"].kotlin {
    srcDir("src/main/kotlin")
    srcDir("../benchmarksReports/src/commonMain/kotlin")
}

val Project.kotlinxBenchmarkVersion: String
    get() = property("kotlinx.benchmark.version") as String

dependencies {
    val kotlinVersion = project.bootstrapKotlinVersion
    val kotlinxBenchmarkVersion = project.kotlinxBenchmarkVersion

    compileOnly(gradleApi())

    implementation(kotlinBuildHelpers())
    implementation(kotlin("gradle-plugin", kotlinVersion))
    implementation(kotlin("stdlib", kotlinVersion))

    implementation("org.jetbrains.kotlinx.benchmark:org.jetbrains.kotlinx.benchmark.gradle.plugin:${kotlinxBenchmarkVersion}")
}

gradlePlugin {
    plugins {
        create("swiftBenchmarking") {
            id = "swift-benchmarking"
            implementationClass = "org.jetbrains.kotlin.benchmark.SwiftBenchmarkingPlugin"
        }
        create("kotlinxBenchmarking") {
            id = "kotlinx-benchmarking"
            implementationClass = "org.jetbrains.kotlin.benchmark.KotlinxBenchmarkingPlugin"
        }
    }
}
