plugins {
    `kotlin-dsl`
    kotlin("jvm")
}

sourceSets["main"].kotlin {
    srcDir("src/main/kotlin")
    srcDir("../benchmarksReports/src/commonMain/kotlin")
}

dependencies {
    val kotlinVersion = project.bootstrapKotlinVersion
    val kotlinxBenchmarkVersion = "0.4.17"

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
