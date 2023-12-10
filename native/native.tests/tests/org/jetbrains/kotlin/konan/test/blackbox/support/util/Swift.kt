/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.target.AppleConfigurables
import org.jetbrains.kotlin.konan.target.withOSVersion
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.configurables
import java.io.File
import kotlin.time.measureTimedValue

internal fun AbstractNativeSimpleTest.compileWithSwiftc(
    outputFile: File,
    sources: List<File>,
    frameworkDirectories: List<File> = emptyList(),
    rpaths: List<String> = emptyList(),
): TestCompilationResult<out TestCompilationArtifact.Executable> {
    val configurables = testRunSettings.configurables
    require(configurables is AppleConfigurables)
    val swiftc = "${configurables.absoluteTargetToolchain}/bin/swiftc"
    val target = configurables.targetTriple.withOSVersion(configurables.osVersionMin).toString()
    val processBuilder = ProcessBuilder(
        swiftc,
        "-o", outputFile.absolutePath,
        *sources.map { it.absolutePath }.toTypedArray(),
        *frameworkDirectories.flatMap { listOf("-F", it.absolutePath) }.toTypedArray(),
        *rpaths.flatMap { listOf("-Xlinker", "-rpath", "-Xlinker", it) }.toTypedArray(),
        "-target", target,
        "-sdk", configurables.absoluteTargetSysRoot,
        // Linker doesn't do adhoc codesigning for tvOS arm64 simulator by default.
        "-Xlinker", "-adhoc_codesign",
        // To fail compilation on warnings in framework header.
        "-Xcc", "-Werror",
        "-g",

    )
    processBuilder.environment()
    val process = processBuilder.start()
    val (exitCode, duration) = measureTimedValue {
        process.waitFor()
    }
    val swiftcErrorOutput = process.errorStream.readBytes()
    val swiftcOutput = process.inputStream.readBytes()
    val parameters = CliToolLoggedData("SWIFTC", processBuilder.command())
    val loggedData = LoggedData.CompilationToolCall(
        toolName = "SWIFTC",
        parameters = parameters,
        exitCode = if (exitCode == 0) ExitCode.OK else ExitCode.COMPILATION_ERROR,
        toolOutput = swiftcOutput.decodeToString() + swiftcErrorOutput.decodeToString(),
        toolOutputHasErrors = swiftcErrorOutput.isNotEmpty(),
        duration = duration
    )
    return if (exitCode != 0) {
        TestCompilationResult.CompilationToolFailure(loggedData)
    } else {
        val executable = TestCompilationArtifact.Executable(outputFile)
        TestCompilationResult.Success(executable, loggedData)
    }
}

internal class CliToolLoggedData(
    private val toolName: String,
    private val command: List<String>,
) : LoggedData() {
    override fun computeText(): String = buildString {
        appendLine(toolName)
        command.forEach {
            appendLine("- $it")
        }
    }
}