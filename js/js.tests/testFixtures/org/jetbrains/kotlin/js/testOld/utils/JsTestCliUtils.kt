/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.utils

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2WasmCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import org.jetbrains.kotlin.config.Services
import kotlin.test.fail

internal fun runJsCompiler(
    messageCollector: MessageCollectorImpl = MessageCollectorImpl(),
    expectedExitCode: ExitCode = ExitCode.OK,
    argsBuilder: K2JSCompilerArguments.() -> Unit,
) {
    val args = K2JSCompilerArguments().apply(argsBuilder)

    val exitCode = K2JSCompiler().exec(messageCollector, Services.EMPTY, args)
    if (exitCode != expectedExitCode) fail(
        buildString {
            appendLine("Unexpected compiler exit code:")
            appendLine("  Expected: $expectedExitCode")
            appendLine("  Actual:   $exitCode")
            appendLine("Command-line arguments: " + args.toArgumentStrings().joinToString(" "))
            appendLine("Compiler output:")
            appendLine(messageCollector.toString())
        }
    )
}

internal fun runWasmCompiler(
    messageCollector: MessageCollectorImpl = MessageCollectorImpl(),
    expectedExitCode: ExitCode = ExitCode.OK,
    argsBuilder: K2WasmCompilerArguments.() -> Unit,
) {
    val args = K2WasmCompilerArguments().apply(argsBuilder)

    val exitCode = K2JSCompiler().exec(messageCollector, Services.EMPTY, args)
    if (exitCode != expectedExitCode) fail(
        buildString {
            appendLine("Unexpected compiler exit code:")
            appendLine("  Expected: $expectedExitCode")
            appendLine("  Actual:   $exitCode")
            appendLine("Command-line arguments: " + args.toArgumentStrings().joinToString(" "))
            appendLine("Compiler output:")
            appendLine(messageCollector.toString())
        }
    )
}
