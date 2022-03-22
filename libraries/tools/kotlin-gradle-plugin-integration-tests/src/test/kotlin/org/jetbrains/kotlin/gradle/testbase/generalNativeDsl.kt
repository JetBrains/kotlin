/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.KOTLIN_VERSION
import org.jetbrains.kotlin.gradle.native.SINGLE_NATIVE_TARGET_PLACEHOLDER
import org.jetbrains.kotlin.gradle.native.configureJvmMemory
import org.jetbrains.kotlin.gradle.native.disableKotlinNativeCaches
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

internal const val MAVEN_LOCAL_URL_PLACEHOLDER = "<mavenLocalUrl>"
internal const val PLUGIN_MARKER_VERSION_PLACEHOLDER = "<pluginMarkerVersion>"

enum class NativeToolSettingsKind(val title: String) {
    COMPILER_CLASSPATH("Classpath"),
    COMMAND_LINE_ARGUMENTS("Arguments"),
    CUSTOM_ENV_VARIABLES("Custom ENV variables")
}

internal fun extractNativeToolSettings(
    output: String,
    toolName: String,
    taskPath: String?,
    settingsKind: NativeToolSettingsKind
): Sequence<String> {
    val settingsPrefix = "${settingsKind.title} = ["
    val settings = output.lineSequence()
        .run {
            if (taskPath != null) dropWhile { "Executing actions for task '$taskPath'" !in it }.drop(1) else this
        }
        .dropWhile {
            check(taskPath == null || "Executing actions for task" !in it) { "Unexpected log line with new Gradle task: $it" }
            "Run in-process tool \"$toolName\"" !in it && "Run \"$toolName\" tool in a separate JVM process" !in it
        }
        .drop(1)
        .dropWhile {
            check(taskPath == null || "Executing actions for task" !in it) { "Unexpected log line with new Gradle task: $it" }
            settingsPrefix !in it
        }

    val settingsHeader = settings.firstOrNull()
    check(settingsHeader != null && settingsPrefix in settingsHeader) {
        "Cannot find setting '${settingsKind.title}' for task ${taskPath}"
    }

    return if (settingsHeader.trimEnd().endsWith(']'))
        emptySequence() // No parameters.
    else
        settings.drop(1).map { it.trim() }.takeWhile { it != "]" }
}

fun BuildResult.extractNativeCommandLineArguments(taskPath: String? = null, toolName: String): List<String> =
    extractNativeToolSettings(output, toolName, taskPath, NativeToolSettingsKind.COMMAND_LINE_ARGUMENTS).toList()

fun BuildResult.extractNativeCompilerClasspath(taskPath: String? = null, toolName: String): List<String> =
    extractNativeToolSettings(output, toolName, taskPath, NativeToolSettingsKind.COMPILER_CLASSPATH).toList()

fun BuildResult.extractNativeCustomEnvironment(taskPath: String? = null, toolName: String): Map<String, String> =
    extractNativeToolSettings(output, toolName, taskPath, NativeToolSettingsKind.CUSTOM_ENV_VARIABLES).map {
        val (key, value) = it.split("=")
        key.trim() to value.trim()
    }.toMap()

fun BuildResult.withNativeCommandLineArguments(
    vararg taskPaths: String,
    toolName: String = "konanc",
    check: (List<String>) -> Unit
) = taskPaths.forEach { taskPath -> check(extractNativeCommandLineArguments(taskPath, toolName)) }

fun BuildResult.withNativeCompilerClasspath(
    vararg taskPaths: String,
    toolName: String = "konanc",
    check: (List<String>) -> Unit
) = taskPaths.forEach { taskPath -> check(extractNativeCompilerClasspath(taskPath, toolName)) }

fun BuildResult.withNativeCustomEnvironment(
    vararg taskPaths: String,
    toolName: String = "konanc",
    check: (Map<String, String>) -> Unit
) = taskPaths.forEach { taskPath -> check(extractNativeCustomEnvironment(taskPath, toolName)) }

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

internal fun KGPBaseTest.transformNativeTestProject(
    projectName: String,
    gradleVersion: GradleVersion,
    directoryPrefix: String? = null
): TestProject {
    val project = project(projectName, gradleVersion, directoryPrefix = directoryPrefix)
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

/** Copies the logic of Gradle [`mavenLocal()`](https://docs.gradle.org/3.4.1/dsl/org.gradle.api.artifacts.dsl.RepositoryHandler.html#org.gradle.api.artifacts.dsl.RepositoryHandler:mavenLocal())
 */
internal object MavenLocalUrlProvider {
    /** The URL that points to the Gradle's mavenLocal() repository. */
    val mavenLocalUrl by lazy {
        val path = propertyMavenLocalRepoPath ?: homeSettingsLocalRepoPath ?: m2HomeSettingsLocalRepoPath ?: defaultM2RepoPath
        File(path).toURI().toString()
    }

    private val homeDir get() = File(System.getProperty("user.home"))

    private fun getLocalRepositoryFromXml(file: File): String? {
        if (!file.isFile)
            return null

        val xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val localRepoNodes = xml.getElementsByTagName("localRepository")

        if (localRepoNodes.length == 0)
            return null

        val content = localRepoNodes.item(0).textContent

        return content.replace("\\$\\{(.*?)\\}".toRegex()) { System.getProperty(it.groupValues[1]) ?: it.value }
    }

    private val propertyMavenLocalRepoPath get() = System.getProperty("maven.repo.local")

    private val homeSettingsLocalRepoPath
        get() = getLocalRepositoryFromXml(File(homeDir, ".m2/settings.xml"))

    private val m2HomeSettingsLocalRepoPath
        get() = System.getProperty("M2_HOME")?.let { getLocalRepositoryFromXml(File(it, "conf/settings.xml")) }

    private val defaultM2RepoPath get() = File(homeDir, ".m2/repository").absolutePath
}