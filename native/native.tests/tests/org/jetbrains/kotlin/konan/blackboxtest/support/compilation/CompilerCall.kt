/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.compilation

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.compilerRunner.processCompilerOutput
import org.jetbrains.kotlin.config.Services
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.InvocationTargetException
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

internal fun callCompiler(compilerArgs: Array<String>, kotlinNativeClassLoader: ClassLoader): CompilerCallResult {
    val compilerXmlOutput: ByteArrayOutputStream
    val exitCode: ExitCode

    @OptIn(ExperimentalTime::class)
    val duration = measureTime {
        val servicesClass = Class.forName(Services::class.java.canonicalName, true, kotlinNativeClassLoader)
        val emptyServices = servicesClass.getField("EMPTY").get(servicesClass)

        val compilerClass = Class.forName("org.jetbrains.kotlin.cli.bc.K2Native", true, kotlinNativeClassLoader)
        val entryPoint = compilerClass.getMethod(
            "execAndOutputXml",
            PrintStream::class.java,
            servicesClass,
            Array<String>::class.java
        )

        compilerXmlOutput = ByteArrayOutputStream()
        exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            val result = entryPoint.invoke(compilerClass.getDeclaredConstructor().newInstance(), printStream, emptyServices, compilerArgs)
            ExitCode.valueOf(result.toString())
        }
    }

    val messageCollector: MessageCollector
    val compilerOutput: String

    ByteArrayOutputStream().use { outputStream ->
        PrintStream(outputStream).use { printStream ->
            messageCollector = GroupingMessageCollector(
                PrintingMessageCollector(printStream, MessageRenderer.SYSTEM_INDEPENDENT_RELATIVE_PATHS, true),
                false
            )
            processCompilerOutput(
                messageCollector,
                OutputItemsCollectorImpl(),
                compilerXmlOutput,
                exitCode
            )
            messageCollector.flush()
        }
        compilerOutput = outputStream.toString(Charsets.UTF_8.name())
    }

    return CompilerCallResult(exitCode, compilerOutput, messageCollector.hasErrors(), duration)
}

internal fun callCinterop(compilerArgs: Array<String>, kotlinNativeClassLoader: ClassLoader): CompilerCallResult {
    val exitCode: ExitCode
    var output = ""

    @OptIn(ExperimentalTime::class)
    val duration = measureTime {
        val compilerClass = Class.forName("org.jetbrains.kotlin.cli.utilities.MainKt", true, kotlinNativeClassLoader)
        val entryPoint = compilerClass.getMethod(
            "main", // FIXME: need daemonMain
            Array<String>::class.java
        )

        exitCode = try {
            entryPoint.invoke(null, arrayOf("cinterop") + compilerArgs)
            ExitCode.OK
        } catch (e: Throwable) {
            val cause = (e as? InvocationTargetException)?.cause ?: e
            val stringWriter = StringWriter()
            val printWriter = PrintWriter(stringWriter)
            cause.printStackTrace(printWriter)
            output = stringWriter.toString()
            ExitCode.COMPILATION_ERROR
        }
    }

    return CompilerCallResult(exitCode, output, exitCode != ExitCode.OK, duration)
}

internal interface CompilerToolRunner {
    fun run(compilerArgs: Array<String>, kotlinNativeClassLoader: ClassLoader): CompilerCallResult
    val toolName: String
}

internal object CompilerRunner : CompilerToolRunner {
    override fun run(compilerArgs: Array<String>, kotlinNativeClassLoader: ClassLoader): CompilerCallResult =
        callCompiler(compilerArgs, kotlinNativeClassLoader)

    override val toolName: String
        get() = "kotlinc-native"
}

internal object CinteropRunner : CompilerToolRunner {
    override fun run(compilerArgs: Array<String>, kotlinNativeClassLoader: ClassLoader): CompilerCallResult =
        callCinterop(compilerArgs, kotlinNativeClassLoader)

    override val toolName: String
        get() = "cinterop"
}

internal data class CompilerCallResult(
    val exitCode: ExitCode,
    val compilerOutput: String,
    val compilerOutputHasErrors: Boolean,
    val duration: Duration
)
