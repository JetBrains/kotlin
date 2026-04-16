import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.`maven-publish`
import plugins.configureDefaultPublishing

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("multiplatform")
    `maven-publish`
}

group = "org.jetbrains.kotlin.commonizer"
description = "Provides numeric expect classes covering inconsistent types in cinterops"

// This library has no test sources. Prevent all test tasks from executing so that the allTests
// lifecycle task (triggered by IntelliJ during debug runs) doesn't attempt to compile and run
// empty native test binaries.
//
// Why onlyIf instead of enabled = false:
//   KGP's KotlinNativeTestRunFactories sets `testTask.enabled = isEnabledOnCurrentHost` during
//   task registration, which overwrites a prior `enabled = false`. onlyIf specs are accumulative
//   — they cannot be cleared by a later `enabled = true`.
//
// Why AbstractTestTask instead of KotlinNativeTest:
//   AbstractTestTask is a Gradle core type, immune to class-loader differences between the
//   bootstrap KGP used to compile this build script and the KGP that actually creates the tasks.
tasks.withType<AbstractTestTask>().configureEach {
    onlyIf("This library has no test sources") { false }
}

configureDefaultPublishing()

val generateSources = tasks.register<GenerateSupportSources>("generateSources") {
    description = "Generates the numeric `expect` classes from the declarations in `src/`."
    sourceTemplateDir.set(layout.projectDirectory.dir("src-template"))
    rawSourceDir.set(layout.projectDirectory.dir("src"))
    outputDir.set(layout.buildDirectory.dir("src-gen"))
}

kotlin {
    linuxArm64()
    linuxX64()
    mingwX64()

    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosArm64()
    tvosArm64()
    tvosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()

    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()

    // Non-native targets: no cinterop types exist on these platforms, so the library compiles to
    // an empty klib. The targets are present so that consumers with these targets (e.g. test-utils)
    // can resolve the artifact from the published Maven metadata.
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class) wasmJs()
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class) wasmWasi()
    jvm()
    js()

    @Suppress("DEPRECATION")
    "Deprecated but used by Coroutines".run {
        macosX64()
        tvosX64()
        watchosX64()
    }

    @Suppress("DEPRECATION")
    "Deprecated but used by cryptography-kotlin".run {
        linuxArm32Hfp()
    }

    sourceSets {
        configureEach {
            val sourceSet = this
            if (sourceSet.name.endsWith("Main")) {
                sourceSet.kotlin.srcDirs(listOf("src-template/${sourceSet.name}/kotlin"))

                val subDirProvider = generateSources.map { task ->
                    task.outputs.files.map { it.resolve(name).resolve("kotlin") }
                }
                kotlin.srcDir(subDirProvider)

                val srcDirToExclude = project.file("src/${sourceSet.name}/kotlin")
                sourceSet.kotlin.exclude { it.file.startsWith(srcDirToExclude) }

                val srcTemplateDirToExclude = project.file("src-template/${sourceSet.name}/kotlin")
                sourceSet.kotlin.exclude { it.file.startsWith(srcTemplateDirToExclude) }
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.add("-XXLanguage:+AllowExpectValueClassesWithNoPrimaryConstructor")
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
    }
}

val commonizerKlibs by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false

    outgoing.artifact(layout.buildDirectory.dir("classes/kotlin")) {
        builtBy(tasks.named("build"))
    }
}
