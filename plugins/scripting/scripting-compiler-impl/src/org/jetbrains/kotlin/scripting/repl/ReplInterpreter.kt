/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmModulePathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.scripting.repl.configuration.ReplConfiguration
import java.io.PrintWriter
import java.net.URLClassLoader
import java.util.concurrent.atomic.AtomicInteger

class ReplInterpreter(
    disposable: Disposable,
    private val configuration: CompilerConfiguration,
    private val replConfiguration: ReplConfiguration
) {
    private val lineNumber = AtomicInteger()

    private val previousIncompleteLines = arrayListOf<String>()

    private val classpathRoots = configuration.getList(CLIConfigurationKeys.CONTENT_ROOTS).mapNotNull { root ->
        when (root) {
            is JvmModulePathRoot -> root.file // TODO: only add required modules
            is JvmClasspathRoot -> root.file
            else -> null
        }
    }
    private val classLoader = ReplClassLoader(URLClassLoader(classpathRoots.map { it.toURI().toURL() }.toTypedArray(), null))

    private val messageCollector = object : MessageCollector {
        private var hasErrors = false
        private val messageRenderer = MessageRenderer.WITHOUT_PATHS

        override fun clear() {
            hasErrors = false
        }

        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
            val msg = messageRenderer.render(severity, message, location).trimEnd()
            with(replConfiguration.writer) {
                when (severity) {
                    CompilerMessageSeverity.EXCEPTION -> sendInternalErrorReport(msg)
                    CompilerMessageSeverity.ERROR -> outputCompileError(msg)
                    CompilerMessageSeverity.STRONG_WARNING -> {
                    } // TODO consider reporting this and two below
                    CompilerMessageSeverity.WARNING -> {
                    }
                    CompilerMessageSeverity.INFO -> {
                    }
                    else -> {
                    }
                }
            }
        }

        override fun hasErrors(): Boolean = hasErrors
    }

    // TODO: add script definition with project-based resolving for IDEA repl
    private val scriptCompiler: ReplCompiler by lazy {
        GenericReplCompiler(
            disposable,
            REPL_LINE_AS_SCRIPT_DEFINITION,
            configuration,
            messageCollector
        )
    }
    private val scriptEvaluator: ReplFullEvaluator by lazy {
        GenericReplCompilingEvaluator(scriptCompiler, classpathRoots, classLoader, null, ReplRepeatingMode.REPEAT_ANY_PREVIOUS)
    }

    private val evalState by lazy { scriptEvaluator.createState() }

    fun eval(line: String): ReplEvalResult {
        val fullText = (previousIncompleteLines + line).joinToString(separator = "\n")

        try {
            val evalRes = scriptEvaluator.compileAndEval(
                evalState,
                ReplCodeLine(lineNumber.getAndIncrement(), 0, fullText),
                null,
                object : InvokeWrapper {
                    override fun <T> invoke(body: () -> T): T = replConfiguration.executionInterceptor.execute(body)
                }
            )

            when {
                evalRes !is ReplEvalResult.Incomplete -> previousIncompleteLines.clear()
                replConfiguration.allowIncompleteLines -> previousIncompleteLines.add(line)
                else -> return ReplEvalResult.Error.CompileTime("incomplete code")
            }
            return evalRes
        } catch (e: Throwable) {
            val writer = PrintWriter(System.err)
            classLoader.dumpClasses(writer)
            writer.flush()
            throw e
        }
    }

    fun dumpClasses(out: PrintWriter) {
        classLoader.dumpClasses(out)
    }

    companion object {
        private val REPL_LINE_AS_SCRIPT_DEFINITION = object : KotlinScriptDefinition(Any::class) {
            override val name = "Kotlin REPL"
        }
    }
}
