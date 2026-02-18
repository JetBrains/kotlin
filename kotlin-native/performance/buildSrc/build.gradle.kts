plugins {
    `kotlin-dsl`
    kotlin("jvm")
}

repositories {
    maven("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
    mavenCentral()
    gradlePluginPortal()
}

sourceSets["main"].kotlin {
    srcDir("src/main/kotlin")
    srcDir("../reports/src/main/kotlin/report")
}

dependencies {
    val kotlinVersion = project.bootstrapKotlinVersion

    compileOnly(gradleApi())

    implementation(kotlin("build-gradle-plugin", kotlinBuildProperties.buildGradlePluginVersion.get()))
    implementation(kotlin("gradle-plugin", kotlinVersion))
    implementation(kotlin("stdlib", kotlinVersion))
}

gradlePlugin {
    plugins {
        create("benchmarkPlugin") {
            id = "benchmarking"
            implementationClass = "org.jetbrains.kotlin.benchmark.KotlinNativeBenchmarkingPlugin"
        }
        create("swiftBenchmarking") {
            id = "swift-benchmarking"
            implementationClass = "org.jetbrains.kotlin.benchmark.SwiftBenchmarkingPlugin"
        }
    }
}
