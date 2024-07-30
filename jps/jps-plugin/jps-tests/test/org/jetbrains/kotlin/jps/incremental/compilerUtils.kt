/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.incremental

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.K2JsIrCompiler
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.jps.build.KotlinBuilder
import org.jetbrains.kotlin.test.kotlinPathsForDistDirectoryForTests
import org.jetbrains.kotlin.utils.PathUtil
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.StringReader

fun createTestingCompilerEnvironment(
    messageCollector: MessageCollector,
    outputItemsCollector: OutputItemsCollectorImpl,
    services: Services
): JpsCompilerEnvironment {
    val paths = PathUtil.kotlinPathsForDistDirectoryForTests
    val wrappedMessageCollector = MessageCollectorToOutputItemsCollectorAdapter(messageCollector, outputItemsCollector)
    return JpsCompilerEnvironment(
        paths,
        services,
        KotlinBuilder.classesToLoadByParent,
        wrappedMessageCollector,
        outputItemsCollector,
        MockProgressReporter
    )
}

fun runJSCompiler(args: K2JSCompilerArguments, env: JpsCompilerEnvironment): ExitCode? {
    val argsArray = ArgumentUtils.convertArgumentsToStringList(args).toTypedArray()

    val stream = ByteArrayOutputStream()
    val out = PrintStream(stream)
    val exitCode = CompilerRunnerUtil.invokeExecMethod(K2JsIrCompiler::class.java.name, argsArray, env, out)
    val reader = BufferedReader(StringReader(stream.toString()))
    CompilerOutputParser.parseCompilerMessagesFromReader(env.messageCollector, reader, env.outputItemsCollector)
    return exitCode as? ExitCode
}

private object MockProgressReporter : ProgressReporter {
    override fun progress(message: String) {
    }

    override fun compilationStarted() {
    }

    override fun clearProgress() {
    }
}