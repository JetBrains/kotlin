import gradle.GradlePluginVariant
import gradle.enableKotlinSerializationPlugin

description = "kotlin-ic-event-report"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("test-inputs-check")
}

configureKotlinCompileTasksGradleCompatibility()
enableKotlinSerializationPlugin()

publish()
sourcesJar()
javadocJar()

kotlin {
    explicitApi()
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi")
    }
}

val serializationVersion = GradlePluginVariant.GRADLE_MIN.compatibleKotlinxJsonSerializationVersion

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compileOnly(kotlin("stdlib", coreDepsVersion))
    api(project(":compiler:build-tools:kotlin-build-tools-api"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${serializationVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${serializationVersion}")
    testImplementation(kotlin("stdlib", coreDepsVersion))
    testImplementation(kotlin("test-junit5", coreDepsVersion))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

projectTests {
    testTask()
}
