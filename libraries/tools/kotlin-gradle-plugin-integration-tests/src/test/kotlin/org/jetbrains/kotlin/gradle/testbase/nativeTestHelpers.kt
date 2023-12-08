/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName
import java.nio.file.Path
import java.util.*

val DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX = HostManager.host.presetName.lowercase(Locale.getDefault())

/**
 * Extracts classpath of given task's output
 *
 * @param taskOutput debug level output of the task
 * @param toolName compiler type
 *
 * @return list of dependencies in classpath
 */
fun extractNativeCompilerClasspath(taskOutput: String, toolName: NativeToolKind): List<String> =
    extractNativeToolSettings(taskOutput, toolName, NativeToolSettingsKind.COMPILER_CLASSPATH).toList()

/**
 * Extracts command line arguments of given task's output
 *
 * @param taskOutput debug level output of the task
 * @param toolName compiler type
 *
 * @return list of command line arguments
 */
fun extractNativeCompilerCommandLineArguments(taskOutput: String, toolName: NativeToolKind): List<String> =
    extractNativeToolSettings(taskOutput, toolName, NativeToolSettingsKind.COMMAND_LINE_ARGUMENTS).toList()

enum class NativeToolKind(val title: String) {
    KONANC("konanc"),
    C_INTEROP("cinterop")
}

enum class NativeToolSettingsKind(val title: String) {
    COMPILER_CLASSPATH("Classpath"),
    COMMAND_LINE_ARGUMENTS("Arguments"),
    CUSTOM_ENV_VARIABLES("Custom ENV variables")
}

fun extractNativeToolSettings(
    taskOutput: String,
    toolName: NativeToolKind,
    settingsKind: NativeToolSettingsKind
): Sequence<String> {
    val settingsPrefix = "${settingsKind.title} = ["
    val settings = taskOutput.lineSequence()
        .dropWhile {
            "Run in-process tool \"${toolName.title}\"" !in it && "Run \"${toolName.title}\" tool in a separate JVM process" !in it
        }
        .drop(1)
        .dropWhile {
            settingsPrefix !in it
        }

    val settingsHeader = settings.firstOrNull()
    check(settingsHeader != null && settingsPrefix in settingsHeader) {
        "Cannot find setting '${settingsKind.title}'"
    }

    return if (settingsHeader.trimEnd().endsWith(']'))
        emptySequence() // No parameters.
    else
        settings.drop(1).map { it.trim() }.takeWhile { it != "]" }
}

internal object MPPNativeTargets {
    val current = when (HostManager.host) {
        KonanTarget.LINUX_X64 -> "linux64"
        KonanTarget.MACOS_X64 -> "macos64"
        KonanTarget.MACOS_ARM64 -> "macosArm64"
        KonanTarget.MINGW_X64 -> "mingw64"
        else -> error("Unsupported host")
    }

    val unsupported = when {
        HostManager.hostIsMingw -> setOf("macos64")
        HostManager.hostIsLinux -> setOf("macos64")
        HostManager.hostIsMac -> emptySet()
        else -> error("Unknown host")
    }

    val supported = listOf("linux64", "macos64", "mingw64").filter { !unsupported.contains(it) }
}

fun computeCacheDirName(
    testTarget: KonanTarget,
    cacheKind: String,
    debuggable: Boolean,
    partialLinkageEnabled: Boolean
) = "$testTarget${if (debuggable) "-g" else ""}$cacheKind${if (partialLinkageEnabled) "-pl" else ""}"

fun TestProject.getFileCache(
    fileProjectName: String,
    fileRelativePath: String,
    fqName: String = "",
    executableProjectName: String = "",
    executableName: String = "debugExecutable",
): Path {
    val cacheFlavor = computeCacheDirName(HostManager.host, NativeCacheKind.STATIC.name, true, true)
    val libCacheDir = getICCacheDir(executableName, executableProjectName).resolve(cacheFlavor).resolve("$fileProjectName-per-file-cache")
    val fileId = cacheFileId(fqName, projectPath.resolve(fileRelativePath).toFile().canonicalPath)
    return libCacheDir.resolve(fileId)
}

private fun TestProject.getICCacheDir(executableName: String, projectName: String = "") =
    (if (projectName == "") projectPath else projectPath.resolve(projectName))
        .resolve("build/kotlin-native-ic-cache/$executableName")

private fun cacheFileId(fqName: String, filePath: String) =
    "${if (fqName == "") "ROOT" else fqName}.${filePath.hashCode().toString(Character.MAX_RADIX)}"
