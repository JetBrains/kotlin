/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.gradle.KOTLIN_VERSION
import org.jetbrains.kotlin.gradle.util.assertProcessRunResult
import org.jetbrains.kotlin.gradle.util.runProcess
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.walk
import kotlin.test.assertEquals

internal sealed class XcodeBuildAction(
    val action: String,
) {
    data object Build : XcodeBuildAction("build")
    data object Test : XcodeBuildAction("test")
    class Archive(val archivePath: String) : XcodeBuildAction("archive")
}

internal fun TestProject.buildXcodeProject(
    xcodeproj: Path,
    scheme: String = "iosApp",
    configuration: String = "Debug",
    destination: String = "generic/platform=iOS Simulator",
    sdk: String = "iphonesimulator",
    action: XcodeBuildAction = XcodeBuildAction.Build,
    testRunEnvironment: Map<String, String> = emptyMap(),
    buildSettingOverrides: Map<String, String> = emptyMap(),
    appendToProperties: () -> String = { "" },
    expectedExitCode: Int = 0,
) {
    prepareForXcodebuild(appendToProperties)

    xcodebuild(
        xcodeproj = xcodeproj,
        scheme = scheme,
        configuration = configuration,
        sdk = sdk,
        destination = destination,
        action = action,
        buildSettingOverrides = buildSettingOverrides,
        testRunEnvironment = testRunEnvironment,
        expectedExitCode = expectedExitCode,
    )
}

internal fun TestProject.xcodebuild(
    workingDir: Path = projectPath,
    xcodeproj: Path? = null,
    workspace: Path? = null,
    scheme: String? = null,
    configuration: String? = null,
    sdk: String? = null,
    arch: String? = null,
    destination: String? = null,
    action: XcodeBuildAction = XcodeBuildAction.Build,
    testRunEnvironment: Map<String, String> = emptyMap(),
    buildSettingOverrides: Map<String, String> = emptyMap(),
    derivedDataPath: Path? = projectPath.resolve("xcodeDerivedData"),
    expectedExitCode: Int = 0,
) {
    xcodebuild(
        cmd = buildList {
            infix fun String.set(value: Any?) {
                if (value != null) {
                    add(this)
                    add(value.toString())
                }
            }

            infix fun String.eq(value: Any?) {
                if (value != null) {
                    add("${this}=$value")
                }
            }

            add("xcodebuild")
            add(action.action)
            "-project" set xcodeproj
            "-workspace" set workspace
            "-scheme" set scheme
            "-configuration" set configuration
            "-sdk" set sdk
            "-arch" set arch
            "-destination" set destination
            "-derivedDataPath" set derivedDataPath

            when (action) {
                is XcodeBuildAction.Build -> {}
                is XcodeBuildAction.Test -> {
                    // Disable parallel testing to output stdout/stderr from tests to xcodebuild
                    add("-parallel-testing-enabled")
                    add("NO")
                }
                is XcodeBuildAction.Archive -> {
                    add("-archivePath")
                    add(action.archivePath)
                }
            }

            buildSettingOverrides.forEach {
                it.key eq it.value
            }
        },
        workingDir = workingDir,
        testRunEnvironment = testRunEnvironment,
        expectedExitCode = expectedExitCode,
    )
}

internal fun TestProject.prepareForXcodebuild(appendToProperties: () -> String = { "" }) {
    overrideMavenLocalIfNeeded()

    gradleProperties
        .takeIf(Path::exists)
        ?.let {
            it.append("kotlin_version=${buildOptions.kotlinVersion}")
            it.append("test_fixes_version=${KOTLIN_VERSION}")
            appendToProperties().let { extraProperties ->
                it.append(extraProperties)
            }
            buildOptions.konanDataDir?.let { konanDataDir ->
                it.append("konan.data.dir=${konanDataDir.toAbsolutePath().normalize()}")
            }
            val configurationCacheFlag = buildOptions.configurationCache.toBooleanFlag(gradleVersion)
            if (configurationCacheFlag != null) {
                it.append("org.gradle.unsafe.configuration-cache=$configurationCacheFlag")
                it.append("org.gradle.unsafe.configuration-cache-problems=${buildOptions.configurationCacheProblems.name.lowercase(Locale.getDefault())}")
            }
        }

    build(":wrapper")
}

private fun TestProject.xcodebuild(
    cmd: List<String>,
    workingDir: Path,
    testRunEnvironment: Map<String, String>,
    expectedExitCode: Int,
) {
    val xcodebuildResult = runProcess(
        cmd = cmd,
        environmentVariables = environmentVariables.environmentalVariables + testRunEnvironment.mapKeys {
            // e.g. TEST_RUNNER_FOO prefixed env gets passed to tests in "xcodebuild test" as FOO env
            "TEST_RUNNER_" + it.key
        },
        workingDir = workingDir.toFile(),
    )
    assertProcessRunResult(xcodebuildResult) {
        assertEquals(expectedExitCode, exitCode, "Exit code mismatch for `xcodebuild`.")
    }
}

private fun TestProject.overrideMavenLocalIfNeeded() {
    val mavenLocalOverride = System.getProperty("maven.repo.local") ?: return

    // Manually adding custom local repo, because the system property is lost when Gradle is invoked through Xcode build phase
    projectPath.walk()
        .filter { it.isRegularFile() && it.name in buildFileNames }
        .forEach { file ->
            file.modify { it.replace("mavenLocal()", "maven { setUrl(\"$mavenLocalOverride\") }") }
        }
}
