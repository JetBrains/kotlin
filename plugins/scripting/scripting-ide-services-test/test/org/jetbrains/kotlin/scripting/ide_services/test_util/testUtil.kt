/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_services.test_util

import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocationWithRange
import org.jetbrains.kotlin.scripting.ide_services.compiler.KJvmReplCompilerWithIdeServices
import java.io.Closeable
import java.util.concurrent.atomic.AtomicInteger
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.BasicJvmReplEvaluator
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.util.LinkedSnippet
import kotlin.script.experimental.jvm.util.toSourceCodePosition
import kotlin.script.experimental.util.get

internal class JvmTestRepl (
    private val compileConfiguration: ScriptCompilationConfiguration = simpleScriptCompilationConfiguration,
    private val evalConfiguration: ScriptEvaluationConfiguration = simpleScriptEvaluationConfiguration,
) : Closeable {
    private val currentLineCounter = AtomicInteger(0)

    fun nextCodeLine(code: String): SourceCode =
        SourceCodeTestImpl(
            currentLineCounter.getAndIncrement(),
            code
        )

    private val replCompiler: KJvmReplCompilerWithIdeServices by lazy {
        KJvmReplCompilerWithIdeServices()
    }

    private val compiledEvaluator: BasicJvmReplEvaluator by lazy {
        BasicJvmReplEvaluator()
    }

    fun compile(code: SourceCode) = runBlocking { replCompiler.compile(code, compileConfiguration) }
    fun complete(code: SourceCode, cursor: Int) = runBlocking { replCompiler.complete(code, cursor.toSourceCodePosition(code), compileConfiguration) }

    fun eval(snippet: LinkedSnippet<out CompiledSnippet>) = runBlocking { compiledEvaluator.eval(snippet, evalConfiguration) }

    override fun close() {

    }

}

internal class SourceCodeTestImpl(number: Int, override val text: String) : SourceCode {
    override val name: String? = "Line_$number"
    override val locationId: String? = "location_$number"
}

@JvmName("iterableToList")
fun <T> ResultWithDiagnostics<Iterable<T>>.toList() = this.valueOrNull()?.toList().orEmpty()

@JvmName("sequenceToList")
fun <T> ResultWithDiagnostics<Sequence<T>>.toList() = this.valueOrNull()?.toList().orEmpty()

internal fun JvmTestRepl.compileAndEval(codeLine: SourceCode): Pair<ResultWithDiagnostics<LinkedSnippet<out CompiledSnippet>>, EvaluatedSnippet?> {

    val compRes = compile(codeLine)

    val evalRes = compRes.valueOrNull()?.let {
        eval(it)
    }
    return compRes to evalRes?.valueOrNull().get()
}

internal fun assertCompileFails(
    repl: JvmTestRepl,
    @Suppress("SameParameterValue")
    line: String
) {
    val compiledSnippet =
        checkCompile(repl, line)

    TestCase.assertNull(compiledSnippet)
}

internal fun assertEvalUnit(
    repl: JvmTestRepl,
    @Suppress("SameParameterValue")
    line: String
) {
    val compiledSnippet =
        checkCompile(repl, line)

    val evalResult = repl.eval(compiledSnippet!!)
    val valueResult = evalResult.valueOrNull().get()

    TestCase.assertNotNull("Unexpected eval result: $evalResult", valueResult)
    TestCase.assertTrue(valueResult!!.result is ResultValue.Unit)
}

internal fun <R> assertEvalResult(repl: JvmTestRepl, line: String, expectedResult: R) {
    val compiledSnippet =
        checkCompile(repl, line)

    val evalResult = repl.eval(compiledSnippet!!)
    val valueResult = evalResult.valueOrNull().get()

    TestCase.assertNotNull("Unexpected eval result: $evalResult", valueResult)
    TestCase.assertTrue(valueResult!!.result is ResultValue.Value)
    TestCase.assertEquals(expectedResult, (valueResult.result as ResultValue.Value).value)
}

internal inline fun <reified R> assertEvalResultIs(repl: JvmTestRepl, line: String) {
    val compiledSnippet =
        checkCompile(repl, line)

    val evalResult = repl.eval(compiledSnippet!!)
    val valueResult = evalResult.valueOrNull().get()

    TestCase.assertNotNull("Unexpected eval result: $evalResult", valueResult)
    TestCase.assertTrue(valueResult!!.result is ResultValue.Value)
    TestCase.assertTrue((valueResult.result as ResultValue.Value).value is R)
}

internal fun checkCompile(repl: JvmTestRepl, line: String): LinkedSnippet<KJvmCompiledScript>? {
    val codeLine = repl.nextCodeLine(line)
    val compileResult = repl.compile(codeLine)
    return compileResult.valueOrNull()
}

internal data class CompilationErrors(
    val message: String,
    val location: CompilerMessageLocationWithRange?
)

internal fun <T> ResultWithDiagnostics<T>.getErrors(): CompilationErrors =
    CompilationErrors(
        reports.joinToString("\n") { report ->
            report.location?.let { loc ->
                CompilerMessageLocationWithRange.create(
                    report.sourcePath,
                    loc.start.line,
                    loc.start.col,
                    loc.end?.line,
                    loc.end?.col,
                    null
                )?.toString()?.let {
                    "$it "
                }
            }.orEmpty() + report.message
        },
        reports.firstOrNull {
            when (it.severity) {
                ScriptDiagnostic.Severity.ERROR -> true
                ScriptDiagnostic.Severity.FATAL -> true
                else -> false
            }
        }?.let {
            val loc = it.location ?: return@let null
            CompilerMessageLocationWithRange.create(
                it.sourcePath,
                loc.start.line,
                loc.start.col,
                loc.end?.line,
                loc.end?.col,
                null
            )
        }
    )

