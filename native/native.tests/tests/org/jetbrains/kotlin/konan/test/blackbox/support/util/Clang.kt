/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.configurables
import java.io.File
import kotlin.time.measureTime

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
): TestCompilationResult<out TestCompilationArtifact.Executable> {
    val clangExecutableName = when (clangMode) {
        ClangMode.C -> "clang"
        ClangMode.CXX -> "clang++"
    }
    val clangPath = when (clangDistribution) {
        ClangDistribution.Toolchain -> "${testRunSettings.configurables.absoluteTargetToolchain}/bin/$clangExecutableName"
        ClangDistribution.Llvm -> "${testRunSettings.configurables.absoluteLlvmHome}/bin/$clangExecutableName"
    }
    val process = ProcessBuilder(
        clangPath,
        *sourceFiles.map { it.absolutePath }.toTypedArray(),
        *includeDirectories.flatMap { listOf("-I", it.absolutePath) }.toTypedArray(),
        "-isysroot", testRunSettings.configurables.absoluteTargetSysRoot,
        "-target", testRunSettings.configurables.targetTriple.toString(),
        "-g", "-fmodules",
        *frameworkDirectories.flatMap { listOf("-F", it.absolutePath) }.toTypedArray(),
        *libraryDirectories.flatMap { listOf("-L", it.absolutePath) }.toTypedArray(),
        *libraries.map { "-l$it" }.toTypedArray(),
        *additionalClangFlags.toTypedArray(),
        "-o", outputFile.absolutePath
    ).start()
    val exitCode: Int
    val duration = measureTime {
        exitCode = process.waitFor()
    }
    val clangErrorOutput = process.errorStream.readBytes()
    val clangOutput = process.inputStream.readBytes()

    val parameters = ClangParameters(
        clangPath,
        sourceFiles,
        outputFile,
        includeDirectories,
        frameworkDirectories,
        libraryDirectories,
        libraries,
        additionalClangFlags
    )
    val loggedData = LoggedData.CompilationToolCall(
        toolName = "CLANG",
        parameters = parameters,
        exitCode = if (exitCode == 0) ExitCode.OK else ExitCode.COMPILATION_ERROR,
        toolOutput = clangOutput.decodeToString() + clangErrorOutput.decodeToString(),
        toolOutputHasErrors = clangErrorOutput.isNotEmpty(),
        duration = duration
    )
    return if (exitCode != 0) {
        TestCompilationResult.CompilationToolFailure(loggedData)
    } else {
        val executable = TestCompilationArtifact.Executable(outputFile)
        TestCompilationResult.Success(executable, loggedData)
    }
}

internal class ClangParameters(
    private val clangPath: String,
    private val sourceFiles: List<File>,
    private val outputFile: File,
    private val includeDirectories: List<File> = emptyList(),
    private val frameworkDirectories: List<File> = emptyList(),
    private val libraryDirectories: List<File> = emptyList(),
    private val libraries: List<String> = emptyList(),
    private val additionalClangFlags: List<String> = emptyList(),
) : LoggedData() {
    override fun computeText(): String = buildString {
        appendLine("CLANG")
        appendLine("- $clangPath")
        appendLine("OUTPUT FILE")
        appendLine("- ${outputFile.absolutePath}")
        appendArguments("SOURCES", sourceFiles.map { it.absolutePath })
        appendArguments("INCLUDE DIRECTORIES", includeDirectories.map { it.absolutePath })
        appendArguments("LIBRARY DIRECTORIES", libraryDirectories.map { it.absolutePath })
        appendArguments("FRAMEWORK DIRECTORIES", frameworkDirectories.map { it.absolutePath })
        appendArguments("LIBRARIES", libraries)
        appendArguments("ADDITIONAL CLANG FLAGS", additionalClangFlags)
    }
}