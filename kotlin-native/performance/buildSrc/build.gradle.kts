import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    `kotlin-dsl`
    kotlin("jvm")
}

repositories {
    maven("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
    mavenCentral()
    gradlePluginPortal()
}

sourceSets.main {
    kotlin.srcDir("../reports/src/main/kotlin/report")
}

tasks.withType<KotlinJvmCompile>().configureEach {
    // kotlin-dsl Gradle plugin, applied above, sets these versions to 1.8.
    // This project is compiled with the bootstrap compiler which doesn't support 1.8 anymore.
    // As a workaround, set the versions to 2.3 explicitly:
    compilerOptions.languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_3)
    compilerOptions.apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_3)
    // An alternative would be to update to Gradle 9.
}


dependencies {
    compileOnly(gradleApi())

    implementation(kotlin("build-gradle-plugin", kotlinBuildProperties.buildGradlePluginVersion))
    implementation(kotlin("gradle-plugin", project.bootstrapKotlinVersion))
}

subprojects {  }

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