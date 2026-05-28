@file:OptIn(TemporaryTestFederationApi::class)

import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.testFederation.TemporaryTestFederationApi

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("project-tests-convention")
    id("test-inputs-check")
}

repositories {
    githubCommit("webassembly", "testsuite")
    githubRelease("webassembly", "wabt", revisionPrefix = "")
}

val wabtVersion = "1.0.19"
val testSuiteRevision = "18f8340"

val gradleOs = org.gradle.internal.os.OperatingSystem.current()
val wabtOS = when {
    gradleOs.isMacOsX -> "macos"
    gradleOs.isWindows -> "windows"
    gradleOs.isLinux -> "ubuntu"
    else -> error("Unsupported OS: $gradleOs")
}

val wabt by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

val testSuite by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    compileOnly(project(":core:util.runtime"))
    implementation(project(":kotlin-util-io"))

    implementation(kotlinStdlib())
    implementation(kotlinxCollectionsImmutable())
    testCompileOnly(kotlinTest("junit"))
    testRuntimeOnly(libs.junit.vintage.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(libs.kotlinx.serialization.json)

    testSuite("webassembly:testsuite:$testSuiteRevision@zip")
    wabt("webassembly:wabt:$wabtVersion:$wabtOS@tar.gz")

    implicitDependencies("webassembly:wabt:$wabtVersion:windows@tar.gz")
    implicitDependencies("webassembly:wabt:$wabtVersion:ubuntu@tar.gz")
    implicitDependencies("webassembly:wabt:$wabtVersion:macos@tar.gz")
}

fun CopySpec.removeFirstLevel() {
    eachFile { relativePath = RelativePath(!isDirectory, *relativePath.segments.drop(1).toTypedArray()) }
    includeEmptyDirs = false
}

val unzipTestSuite by task<Sync> {
    dependsOn(testSuite)
    from({ zipTree(testSuite.singleFile) }) {
        removeFirstLevel()
    }
    into(layout.buildDirectory.dir("testsuite"))
}

val unzipWabt by task<Sync> {
    dependsOn(wabt)
    from({ tarTree(resources.gzip(wabt.singleFile)) }) {
        removeFirstLevel()
    }
    into(layout.buildDirectory.dir("wabt"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.optIn.add("kotlin.ExperimentalUnsignedTypes")
    compilerOptions.freeCompilerArgs.add("-Xskip-prerelease-check")
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        addDirectoryProperty("wabt.bin.path") {
            fileProvider(unzipWabt.map { it.destinationDir.resolve("bin") })
        }
        addDirectoryProperty("wasm.testsuite.path") {
            fileProvider(unzipTestSuite.map { it.destinationDir })
        }
    }
}

testsJar()
