/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.tools

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import java.io.ByteArrayOutputStream
import java.io.PrintStream

internal fun runCompilerViaCLI(vararg compilerArgs: List<String?>?) {
    val allCompilerArgs = compilerArgs.flatMap { args -> args.orEmpty().filterNotNull() }.toTypedArray()

    val compilerXmlOutput = ByteArrayOutputStream()
    val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
        K2JSCompiler().execFullPathsInMessages(printStream, allCompilerArgs)
    }

    if (exitCode != ExitCode.OK)
        throw AssertionError(
            buildString {
                appendLine("Compiler failure.")
                appendLine("Exit code = $exitCode.")
                appendLine("Compiler messages:")
                appendLine("==========")
                appendLine(compilerXmlOutput.toString(Charsets.UTF_8.name()))
                appendLine("==========")
            }
        )
}


