/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.gradle.util.assertProcessRunResult
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.gradle.util.runProcess
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.test.assertEquals

internal enum class XcodeBuildMode {
    NORMAL,
    TEST
}

internal fun TestProject.buildXcodeProject(
    xcodeproj: Path,
    scheme: String = "iosApp",
    configuration: String = "Debug",
    destination: String = "generic/platform=iOS Simulator",
    sdk: String = "iphonesimulator",
    buildMode: XcodeBuildMode = XcodeBuildMode.NORMAL,
    extraArguments: Map<String, Any> = emptyMap(),
) {
    prepareForXcodebuild()

    xcodebuild(
        xcodeproj = xcodeproj,
        scheme = scheme,
        configuration = configuration,
        sdk = sdk,
        destination = destination,
        buildMode = buildMode,
        extraArguments = extraArguments
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
    buildMode: XcodeBuildMode = XcodeBuildMode.NORMAL,
    extraArguments: Map<String, Any> = emptyMap(),
    derivedDataPath: Path? = projectPath.resolve("xcodeDerivedData"),
) {
    xcodebuild(
        buildList {
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
            "-project" set xcodeproj
            "-workspace" set workspace
            "-scheme" set scheme
            "-configuration" set configuration
            "-sdk" set sdk
            "-arch" set arch
            "-destination" set destination
            "-derivedDataPath" set derivedDataPath

            extraArguments.forEach {
                it.key eq it.value
            }

            if (buildMode == XcodeBuildMode.TEST) {
                add("test")
            }
        },
        workingDir,
    )
}

internal fun TestProject.prepareForXcodebuild() {
    overrideMavenLocalIfNeeded()

    gradleProperties
        .takeIf(Path::exists)
        ?.let {
            it.append("kotlin_version=${buildOptions.kotlinVersion}")
            it.append("test_fixes_version=${buildOptions.kotlinVersion}")
            buildOptions.konanDataDir?.let { konanDataDir ->
                it.append("konan.data.dir=${konanDataDir.toAbsolutePath().normalize()}")
            }
        }

    build(":wrapper")
}

private fun TestProject.xcodebuild(cmd: List<String>, workingDir: Path) {
    val xcodebuildResult = runProcess(
        cmd = cmd,
        environmentVariables = environmentVariables.environmentalVariables,
        workingDir = workingDir.toFile(),
    )
    assertProcessRunResult(xcodebuildResult) {
        assertEquals(0, exitCode, "Exit code mismatch for `xcodebuild`.")
    }
}

private fun TestProject.overrideMavenLocalIfNeeded() {
    val mavenLocalOverride = System.getProperty("maven.repo.local") ?: return

    // Manually adding custom local repo, because the system property is lost when Gradle is invoked through Xcode build phase
    projectPath.toFile().walkTopDown()
        .filter { it.isFile && it.name in buildFileNames }
        .forEach { file ->
            file.modify { it.replace("mavenLocal()", "maven { setUrl(\"$mavenLocalOverride\") }") }
        }
}