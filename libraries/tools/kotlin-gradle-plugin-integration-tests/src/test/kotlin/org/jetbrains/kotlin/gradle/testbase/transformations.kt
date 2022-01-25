/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.KOTLIN_VERSION
import org.jetbrains.kotlin.gradle.MavenLocalUrlProvider
import org.jetbrains.kotlin.gradle.native.SINGLE_NATIVE_TARGET_PLACEHOLDER
import org.jetbrains.kotlin.gradle.native.configureJvmMemory
import org.jetbrains.kotlin.gradle.native.disableKotlinNativeCaches
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

internal const val MAVEN_LOCAL_URL_PLACEHOLDER = "<mavenLocalUrl>"
internal const val PLUGIN_MARKER_VERSION_PLACEHOLDER = "<pluginMarkerVersion>"

/**
 * Modify file content under [Path].
 *
 * @param transform function receiving current file content and outputting new file content
 */
fun Path.modify(transform: (currentContent: String) -> String) {
    assert(Files.isRegularFile(this)) { "$this is not a regular file!" }

    val file = toFile()
    file.writeText(transform(file.readText()))
}

/**
 * Append [textToAppend] to the file content under [Path].
 */
fun Path.append(
    textToAppend: String
) {
    modify {
        """
            $it
            
            $textToAppend
        """.trimIndent()
    }
}

internal fun KGPBaseTest.transformNativeTestProjectWithPluginDsl(
    projectName: String,
    gradleVersion: GradleVersion,
    directoryPrefix: String? = null
): TestProject {
    val project = transformProjectWithPluginsDsl(projectName, gradleVersion, directoryPrefix = directoryPrefix)
    project.configureSingleNativeTarget()
    project.gradleProperties().apply {
        configureJvmMemory()
        disableKotlinNativeCaches()
    }
    return project
}

private fun TestProject.configureSingleNativeTarget(preset: String = HostManager.host.presetName) {
    projectPath.toFile().walk()
        .filter { it.isFile && (it.name == "build.gradle.kts" || it.name == "build.gradle") }
        .forEach { file ->
            file.modify {
                it.replace(SINGLE_NATIVE_TARGET_PLACEHOLDER, preset)
            }
        }
}

internal fun KGPBaseTest.transformProjectWithPluginsDsl(
    projectName: String,
    gradleVersion: GradleVersion,
    directoryPrefix: String? = null,
    minLogLevel: LogLevel = LogLevel.DEBUG
): TestProject {

    val result = project(
        projectName,
        gradleVersion,
        buildOptions = defaultBuildOptions.copy(logLevel = minLogLevel),
        directoryPrefix = directoryPrefix
    )

    val settingsGradle = File(result.projectPath.toFile(), "settings.gradle").takeIf(File::exists)
    settingsGradle?.modify {
        it.replace(MAVEN_LOCAL_URL_PLACEHOLDER, MavenLocalUrlProvider.mavenLocalUrl)
    }

    result.projectPath.toFile().walkTopDown()
        .filter {
            it.isFile && (it.name == "build.gradle" || it.name == "build.gradle.kts" ||
                    it.name == "settings.gradle" || it.name == "settings.gradle.kts")
        }
        .forEach { buildGradle ->
            buildGradle.modify(::transformBuildScriptWithPluginsDsl)
        }

    return result
}

internal fun transformBuildScriptWithPluginsDsl(buildScriptContent: String): String =
    buildScriptContent.replace(PLUGIN_MARKER_VERSION_PLACEHOLDER, KOTLIN_VERSION)
