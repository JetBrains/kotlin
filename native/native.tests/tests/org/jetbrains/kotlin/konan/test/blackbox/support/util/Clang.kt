/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.target.ClangArgs
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.buildDir
import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.configurables
import java.io.File
import kotlin.time.measureTimedValue

/**
 * Specifies which Clang should be used for compilation.
 */
internal enum class ClangDistribution {
    /**
     * Use Clang from target's toolchain.
     * Note that not all targets do have a toolchain with Clang.
     * For example, Apple targets do (from Xcode), and GCC-based Linux targets do not.
     */
    Toolchain,

    /**
     * Use Clang from Kotlin/Native LLVM distribution.
     */
    Llvm
}

internal enum class ClangMode {
    C, CXX
}

// FIXME: absoluteTargetToolchain might not work correctly with KONAN_USE_INTERNAL_SERVER because
// :kotlin-native:dependencies:update is not a dependency of :native:native.tests:test where this test runs
internal fun AbstractNativeSimpleTest.compileWithClang(
    clangDistribution: ClangDistribution = ClangDistribution.Llvm,
    clangMode: ClangMode = ClangMode.C,
    sourceFiles: List<File>,
    outputFile: File,
    includeDirectories: List<File> = emptyList(),
    frameworkDirectories: List<File> = emptyList(),
    libraryDirectories: List<File> = emptyList(),
    libraries: List<String> = emptyList(),
    additionalClangFlags: List<String> = emptyList(),
    fmodules: Boolean = true
): TestCompilationResult<out TestCompilationArtifact.Executable> {
    val configurables = testRunSettings.configurables
    val host = testRunSettings.get<KotlinNativeTargets>().hostTarget
    val clangExecutableName = when (clangMode) {
        ClangMode.C -> "clang"
        ClangMode.CXX -> "clang++"
    }.let { if (host.family == Family.MINGW) "$it.exe" else it }
    val clangPath = when (clangDistribution) {
        ClangDistribution.Toolchain -> "${configurables.absoluteTargetToolchain}/bin/$clangExecutableName"
        ClangDistribution.Llvm -> "${configurables.absoluteLlvmHome}/bin/$clangExecutableName"
    }
    val clangArgsProvider = ClangArgs.Native(configurables)
    val clangArgs = when (clangMode) {
        ClangMode.C -> clangArgsProvider.clangArgs
        ClangMode.CXX -> clangArgsProvider.clangXXArgs
    }
    val processBuilder = ProcessBuilder(
        clangPath,
        *clangArgs,
        *sourceFiles.map { it.absolutePath }.toTypedArray(),
        *includeDirectories.flatMap { listOf("-I", it.absolutePath) }.toTypedArray(),
        "-g",
        *(if (fmodules) arrayOf("-fmodules") else arrayOf()),
        *frameworkDirectories.flatMap { listOf("-F", it.absolutePath) }.toTypedArray(),
        *libraryDirectories.flatMap { listOf("-L", it.absolutePath) }.toTypedArray(),
        *libraries.map { "-l$it" }.toTypedArray(),
        *additionalClangFlags.toTypedArray(),
        "-o", outputFile.absolutePath
    )
    val process = processBuilder.start()
    val (exitCode, duration) = measureTimedValue {
        process.waitFor()
    }
    val clangErrorOutput = process.errorStream.readBytes()
    val clangOutput = process.inputStream.readBytes()

    val parameters = ClangParameters(
        processBuilder.command()
    )
    val loggedData = LoggedData.CompilationToolCall(
        toolName = "CLANG",
        parameters = parameters,
        exitCode = if (exitCode == 0) ExitCode.OK else ExitCode.COMPILATION_ERROR,
        toolOutput = clangOutput.decodeToString() + clangErrorOutput.decodeToString(),
        toolOutputHasErrors = clangErrorOutput.isNotEmpty(),
        duration = duration,
        input = null,
    )
    return if (exitCode != 0) {
        TestCompilationResult.CompilationToolFailure(loggedData)
    } else {
        val executable = TestCompilationArtifact.Executable(outputFile)
        TestCompilationResult.Success(executable, loggedData)
    }
}

internal class ClangParameters(
    private val command: List<String>,
) : LoggedData() {
    override fun computeText(): String = buildString {
        appendLine("CLANG")
        command.forEach {
            appendLine("- $it")
        }
    }
}

internal fun createModuleMap(directory: File, umbrellaHeader: File): File {
    return directory.resolve("module.modulemap").apply {
        writeText("""
            module KotlinBridges {
                umbrella header "${umbrellaHeader.absolutePath}"
                export *
            }
            """.trimIndent()
        )
    }
}