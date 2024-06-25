/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.utils

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.test.utils.TestMessageCollector
import kotlin.test.fail

internal fun runJsCompiler(
    messageCollector: TestMessageCollector = TestMessageCollector(),
    argsBuilder: K2JSCompilerArguments.() -> Unit,
) {
    val args = K2JSCompilerArguments().apply(argsBuilder)

    val exitCode = K2JSCompiler().exec(messageCollector, Services.EMPTY, args)
    if (exitCode != ExitCode.OK) fail(
        buildString {
            appendLine("Compilation failed with exit code: $exitCode")
            appendLine("Compiler output:")
            appendLine(messageCollector.toString())
        }
    )
}
