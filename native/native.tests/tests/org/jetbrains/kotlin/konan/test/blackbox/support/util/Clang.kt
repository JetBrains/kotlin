/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.target.ClangArgs
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.MingwConfigurables
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.CompilationToolCallResult
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.configurables
import org.jetbrains.kotlin.native.executors.RunProcessException
import org.jetbrains.kotlin.native.executors.runProcess
import java.io.File
import kotlin.time.Duration.Companion.minutes
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
    val clangArguments = buildList<String> {
        val clangArgsProvider = ClangArgs.Native(configurables)
        addAll(
            when (clangMode) {
                ClangMode.C -> clangArgsProvider.clangArgs
                ClangMode.CXX -> clangArgsProvider.clangXXArgs
            }
        )
        addAll(sourceFiles.map { it.absolutePath })
        addAll(includeDirectories.flatMap { listOf("-I", it.absolutePath) })
        add("-g")
        if (fmodules) add("-fmodules")
        addAll(frameworkDirectories.flatMap { listOf("-F", it.absolutePath) })
        addAll(libraryDirectories.flatMap { listOf("-L", it.absolutePath) }.toTypedArray())
        addAll(libraries.map { "-l$it" })
        if (configurables.target.family == Family.LINUX)
            add("-lpthread") // libpthread.so.0: error adding symbols: DSO missing from command line. Maybe because of old llvm.
        if (configurables is MingwConfigurables) {
            add("-femulated-tls") // https://youtrack.jetbrains.com/issue/KT-46612
            // error: argument unused during compilation: '-static-libstdc++'
            add("-Wno-unused-command-line-argument")
            addAll(configurables.linkerKonanFlags)
        }
        addAll(additionalClangFlags)
        add("-o")
        add(outputFile.absolutePath)
    }
    val compilationToolCallResult = try {
        val result = runProcess(clangPath, *clangArguments.toTypedArray()) {
            timeout = 5.minutes
        }
        CompilationToolCallResult(
            exitCode = ExitCode.OK,
            toolOutput = result.output,
            toolOutputHasErrors = result.stderr.isNotEmpty(),
            duration = result.executionTime,
        )
    } catch (rpe: RunProcessException) {
        if (rpe.exitCode == null)
            throw rpe // Treat compiler timeouts as fatal errors
        CompilationToolCallResult(
            exitCode = ExitCode.COMPILATION_ERROR,
            toolOutput = rpe.output,
            toolOutputHasErrors = rpe.stderr.isNotEmpty(),
            duration = rpe.executionTime,
        )
    }

    val loggedData = LoggedData.CompilationToolCall(
        toolName = "CLANG",
        parameters = CommandParameters(
            commandName = "CLANG",
            command = listOf(clangPath) + clangArguments
        ),
        exitCode = compilationToolCallResult.exitCode,
        toolOutput = compilationToolCallResult.toolOutput,
        toolOutputHasErrors = compilationToolCallResult.toolOutputHasErrors,
        duration = compilationToolCallResult.duration,
        input = null,
    )
    return if (loggedData.exitCode != ExitCode.OK) {
        TestCompilationResult.CompilationToolFailure(loggedData)
    } else {
        val executable = TestCompilationArtifact.Executable(outputFile)
        TestCompilationResult.Success(executable, loggedData)
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