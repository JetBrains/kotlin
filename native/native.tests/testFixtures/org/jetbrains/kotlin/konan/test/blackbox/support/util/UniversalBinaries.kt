/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.target.AppleConfigurables
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.isSimulator
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.configurables
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.toolsExecutor
import org.jetbrains.kotlin.native.executors.runProcess
import java.io.File
import java.io.FileInputStream

/**
 * Architectures of the slices of a universal binary for the (Apple) test target:
 * the target's own architecture first, then another one that the target SDK supports.
 */
internal val AbstractNativeSimpleTest.universalBinaryArchs: List<String>
    get() {
        val configurables = testRunSettings.configurables as AppleConfigurables
        val targetArch = configurables.arch
        val otherArch = when {
            configurables.target.family == Family.OSX || configurables.targetTriple.isSimulator ->
                if (targetArch == "x86_64") "arm64" else "x86_64"
            // Apple devices SDKs don't support x86_64, but support arm64e.
            else -> "arm64e"
        }
        return listOf(targetArch, otherArch)
    }

/**
 * Creates the universal binary [outputFile] from the slices that [compileSlice] produces for each of [archs].
 * A single slice also makes a universal binary.
 */
internal fun AbstractNativeSimpleTest.createUniversalBinary(
    archs: List<String>,
    outputFile: File,
    compileSlice: (arch: String) -> File,
): File {
    val slices = archs.map(compileSlice)
    if (outputFile.exists()) outputFile.delete()
    return lipoCreate(
        inputFiles = slices,
        outputFile = outputFile,
    ).assertSuccess().resultingArtifact.libraryFile
}

private fun AbstractNativeSimpleTest.lipoCreate(
    inputFiles: List<File>,
    outputFile: File,
): TestCompilationResult<out TestCompilationArtifact.BinaryLibrary> {
    val lipoPath = "${testRunSettings.configurables.absoluteTargetToolchain}/bin/lipo"
    val arguments = arrayOf(
        "-create",
        *inputFiles.map { it.canonicalPath }.toTypedArray(),
        "-output",
        outputFile.canonicalPath,
    )
    val result = toolsExecutor.runProcess(lipoPath, *arguments)

    val parameters = CommandParameters(
        commandName = "LIPO",
        command = listOf(lipoPath) + arguments.toList()
    )

    fun loggedData(output: String = "") = LoggedData.CompilationToolCall(
        toolName = "LIPO",
        parameters = parameters,
        exitCode = ExitCode.OK,
        toolOutput = result.output,
        toolOutputHasErrors = result.stderr.isNotEmpty(),
        duration = result.executionTime,
        input = null,
    )

    if (!outputFile.exists()) {
        return TestCompilationResult.CompilationToolFailure(
            loggedData(
                "\nMissing output library"
            )
        )
    }

    val universalMagic = listOf(0xca, 0xfe, 0xba, 0xbe)
    val actualMagic = FileInputStream(outputFile).use { stream -> (0..<universalMagic.count()).map { stream.read() } }
    if (actualMagic != universalMagic) {
        return TestCompilationResult.CompilationToolFailure(
            loggedData(
                "\nLipo output was not a universal image"
            )
        )
    }

    return TestCompilationResult.Success(
        TestCompilationArtifact.BinaryLibrary(outputFile),
        loggedData(),
    )
}
