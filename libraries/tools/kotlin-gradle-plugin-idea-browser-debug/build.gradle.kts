import gradle.GradlePluginVariant
import gradle.enableKotlinSerializationPlugin

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    `maven-publish`
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("project-tests-convention")
    id("test-inputs-check")
}

description = "Communication Library to coordinate debug session between IDEA and Kotlin Gradle Plugin"

configureKotlinCompileTasksGradleCompatibility()
enableKotlinSerializationPlugin()

extensions.extraProperties["kotlin.stdlib.default.dependency"] = "false"
kotlin {
    coreLibrariesVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi")
    }
}

val serializationVersion = GradlePluginVariant.GRADLE_MIN.compatibleKotlinxJsonSerializationVersion

dependencies {
    compileOnly(kotlin("stdlib", kotlin.coreLibrariesVersion))
    api(project(":kotlin-gradle-plugin-annotations"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")

    testImplementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
    testRuntimeOnly(kotlin("test-junit5"))
}

publish()
standardPublicJars()

apiValidation {
    nonPublicMarkers += "org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi"
}

tasks {
    apiBuild {
        inputJar.value(jar.flatMap { it.archiveFile })
    }
}

projectTests {
    testTask()
}
