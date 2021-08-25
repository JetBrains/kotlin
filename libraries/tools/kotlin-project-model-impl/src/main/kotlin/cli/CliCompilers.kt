/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx.cli

import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.isIrBackendEnabled
import org.jetbrains.kotlin.cli.common.arguments.isPreIrBackendDisabled
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.js.K2JsIrCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.project.modelx.compiler.Compilers
import java.io.OutputStream

/**
 * Compilers that spawn new process in OS to execute compilation
 */
fun createCliCompilers(
    output: OutputStream
): Compilers {
    val k2MetadataCompiler = K2MetadataCompiler()
    val k2JvmCompiler = K2JVMCompiler()
    val k2JsCompiler = K2JSCompiler()
    val k2JsIrCompiler = K2JsIrCompiler()

    val collector = OutputMessageCollector(output)

    return Compilers(
        compileMetadata = { args ->
            println("Compile metadata with args: ${args.toPrettyString()}")
            k2MetadataCompiler.exec(collector, Services.EMPTY, args).also { collector.flush() }
        },
        compileJvm = { args ->
            println("Compile JVM with args: ${args.toPrettyString()}")
            k2JvmCompiler.exec(collector, Services.EMPTY, args).also { collector.flush() }
        },
        compileJs = { args ->
            println("Compile JS with args: ${args.toPrettyString()}")
            when {
                args.isIrBackendEnabled() -> k2JsIrCompiler.exec(collector, Services.EMPTY, args).also { collector.flush() }
                !args.isPreIrBackendDisabled() -> k2JsCompiler.exec(collector, Services.EMPTY, args).also { collector.flush() }
                else -> error("No JS Compiler is called.")
            }
        },
        // compileNative = { TODO() }
    )
}

private class OutputMessageCollector(
    output: OutputStream
) : MessageCollector {
    private val writer = output.bufferedWriter()

    fun flush() {
        writer.flush()
    }

    override fun clear() = Unit

    override fun hasErrors(): Boolean = false

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        writer.write("[${severity.name}] $message")
        if (location != null) {
            writer.write(" at $location")
        }
        writer.newLine()
    }
}

private fun CommonToolArguments.toPrettyString() = ArgumentUtils
    .convertArgumentsToStringListNoDefaults(this)
    .joinToString(" ")