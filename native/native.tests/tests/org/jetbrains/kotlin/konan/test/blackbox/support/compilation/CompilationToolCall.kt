/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.compilation

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.compilerRunner.processCompilerOutput
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.time.measureTimedValue
import org.jetbrains.kotlin.konan.file.File as KonanFile

internal data class CompilationToolCallResult(
    val exitCode: ExitCode,
    val toolOutput: String,
    val toolOutputHasErrors: Boolean,
    val duration: Duration
)

internal fun callCompiler(compilerArgs: Array<String>, kotlinNativeClassLoader: ClassLoader): CompilationToolCallResult {
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
            messageCollector = NativeTestGroupingMessageCollector(
                compilerArgs = compilerArgs,
                delegate = PrintingMessageCollector(printStream, MessageRenderer.SYSTEM_INDEPENDENT_RELATIVE_PATHS, /*verbose =*/ true),
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

    return CompilationToolCallResult(exitCode, compilerOutput, messageCollector.hasErrors(), duration)
}

internal fun callCompilerWithoutOutputInterceptor(
    compilerArgs: Array<String>,
    kotlinNativeClassLoader: ClassLoader
): CompilationToolCallResult {
    val exitCode: ExitCode
    val compilerOutput: String

    @OptIn(ExperimentalTime::class)
    val duration = measureTime {
        val compilerClass = Class.forName("org.jetbrains.kotlin.cli.bc.K2Native", true, kotlinNativeClassLoader)
        val entryPoint = compilerClass.getMethod(
            "exec",
            PrintStream::class.java,
            Array<String>::class.java
        )
        // TODO: we better use the same entry point as the actual compiler.

        val outputStream = ByteArrayOutputStream()

        exitCode = PrintStream(outputStream).use { printStream ->
            val result = entryPoint.invoke(compilerClass.getDeclaredConstructor().newInstance(), printStream, compilerArgs)
            ExitCode.valueOf(result.toString())
        }

        compilerOutput = outputStream.toString(Charsets.UTF_8.name())
    }

    val toolOutputHasErrors = exitCode != ExitCode.OK // An approximation, good enough.
    // Alternatively, we can look for 'error:' and 'exception:' in the output.

    return CompilationToolCallResult(exitCode, compilerOutput, toolOutputHasErrors = toolOutputHasErrors, duration)
}

@OptIn(ExperimentalTime::class)
internal fun invokeCInterop(
    kotlinNativeClassLoader: ClassLoader,
    targets: KotlinNativeTargets,
    inputDef: File,
    outputLib: File,
    extraArgs: Array<String>
): CompilationToolCallResult {
    val args = arrayOf("-o", outputLib.canonicalPath, "-def", inputDef.canonicalPath, "-no-default-libs", "-target", targets.testTarget.name)
    val buildDir = KonanFile("${outputLib.canonicalPath}-build")
    val generatedDir = KonanFile(buildDir, "kotlin")
    val nativesDir = KonanFile(buildDir, "natives")
    val manifest = KonanFile(buildDir, "manifest.properties")
    val cstubsName = "cstubs"

    val interopClass = Class.forName("org.jetbrains.kotlin.native.interop.gen.jvm.Interop", true, kotlinNativeClassLoader)
    val entryPoint = interopClass.declaredMethods.single { it.name == "interopViaReflection" }

    val (cinteropResult, duration) = measureTimedValue {
        entryPoint.invoke(
            interopClass.getDeclaredConstructor().newInstance(),
            "native",
            args + extraArgs,
            false,
            generatedDir.absolutePath, nativesDir.absolutePath, manifest.path, cstubsName // args for InternalInteropOptions()
        )
    }
    return when {
        // cinterop has failed with a known error that was returned as a result.
        cinteropResult is Exception -> {
            CompilationToolCallResult(
                exitCode = ExitCode.COMPILATION_ERROR,
                toolOutput = cinteropResult.message ?: "",
                toolOutputHasErrors = true,
                duration
            )
        }
        // In currently tested usecases, cinterop must return no args for the subsequent compiler call
        cinteropResult is Array<*> -> {
            CompilationToolCallResult(
                exitCode = ExitCode.COMPILATION_ERROR,
                toolOutput = cinteropResult.joinToString(" "),
                toolOutputHasErrors = true,
                duration
            )
        }
        else -> {
            // TODO There is no technical ability to extract `toolOutput` and `toolOutputHasErrors`
            //      from C-interop tool invocation at the moment. This should be fixed in the future.
            CompilationToolCallResult(exitCode = ExitCode.OK, toolOutput = "", toolOutputHasErrors = false, duration)
        }
    }
}
