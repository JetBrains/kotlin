/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.test.testFramework.resetApplicationToNull
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.parallel.Resources
import java.io.Closeable
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.script.experimental.jvmhost.repl.JvmReplCompiler
import kotlin.script.experimental.jvmhost.repl.JvmReplEvaluator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Adapted form GenericReplTest

// Artificial split into several testsuites, to speed up parallel testing
@ResourceLock(Resources.SYSTEM_OUT)
class LegacyReplTest {
    @Test
    fun testReplBasics() {
        LegacyTestRepl().use { repl ->
            val res1 = repl.replCompiler.compile(repl.state, ReplCodeLine(0, 0, "val x ="))
            assertTrue(res1 is ReplCompileResult.Incomplete, "Unexpected check results: $res1")

            assertEvalResult(repl, "val l1 = listOf(1 + 2)\nl1.first()", 3)

            assertEvalUnit(repl, "val x = 5")

            assertEvalResult(repl, "x + 2", 7)
        }
    }

    @Test
    fun testReplErrors() {
        LegacyTestRepl().use { repl ->
            repl.compileAndEval(repl.nextCodeLine("val x = 10"))

            val res = repl.compileAndEval(repl.nextCodeLine("java.util.fish"))
            assertTrue(res.first is ReplCompileResult.Error, "Expected compile error")

            val result = repl.compileAndEval(repl.nextCodeLine("x"))
            assertEquals(10, (result.second as ReplEvalResult.ValueResult).value, res.second.toString())
        }
    }

    @Test
    fun testReplSyntaxErrorsChecked() {
        LegacyTestRepl().use { repl ->
            val res = repl.compileAndEval(repl.nextCodeLine("data class Q(val x: Int, val: String)"))
            assertTrue(res.first is ReplCompileResult.Error, "Expected compile error")
        }
    }

    @Test
    fun testReplCodeFormat() {
        LegacyTestRepl().use { repl ->
            val codeLine0 = ReplCodeLine(0, 0, "val l1 = 1\r\nl1\r\n")
            val res0 = repl.replCompiler.compile(repl.state, codeLine0)
            val res0c = res0 as? ReplCompileResult.CompiledClasses
            assertNotNull(res0c, "Unexpected compile result: $res0")
        }
    }

    @Test
    fun testRepPackage() {
        LegacyTestRepl().use { repl ->
            assertEvalResult(repl, "package mypackage\n\nval x = 1\nx+2", 3)

            assertEvalResult(repl, "x+4", 5)
        }
    }

    @Test
    fun testReplResultFieldWithFunction() {
        LegacyTestRepl().use { repl ->
            assertEvalResultIs<Function0<Int>>(repl, "{ 1 + 2 }")
            assertEvalResultIs<Function0<Int>>(repl, "res0")
            assertEvalResult(repl, "res0()", 3)
        }
    }

    @Test
    fun testReplResultField() {
        LegacyTestRepl().use { repl ->
            assertEvalResult(repl, "5 * 4", 20)
            assertEvalResult(repl, "res0 + 3", 23)
        }
    }
}

// Artificial split into several testsuites, to speed up parallel testing
@ResourceLock(Resources.SYSTEM_OUT)
class LegacyReplTestLong1 {

    @Test
    fun test256Evals() {
        LegacyTestRepl().use { repl ->
            repl.compileAndEval(ReplCodeLine(0, 0, "val x0 = 0"))

            val evals = 256
            for (i in 1..evals) {
                repl.compileAndEval(ReplCodeLine(i, 0, "val x$i = x${i-1} + 1"))
            }

            val res = repl.compileAndEval(ReplCodeLine(evals + 1, 0, "x$evals"))
            assertEquals(evals, (res.second as? ReplEvalResult.ValueResult)?.value, res.second.toString())
        }
    }
}

// Artificial split into several testsuites, to speed up parallel testing
@ResourceLock(Resources.SYSTEM_OUT)
class LegacyReplTestLong2 {

    @Test
    fun testReplSlowdownKt22740() {
        LegacyTestRepl().use { repl ->
            repl.compileAndEval(ReplCodeLine(0, 0, "class Test<T>(val x: T) { fun <R> map(f: (T) -> R): R = f(x) }".trimIndent()))

            // We expect that analysis time is not exponential
            for (i in 1..60) {
                repl.compileAndEval(ReplCodeLine(i, 0, "fun <T> Test<T>.map(f: (T) -> Double): List<Double> = listOf(f(this.x))"))
            }
        }
    }
}

internal class LegacyTestRepl : Closeable {
    val application = ApplicationManager.getApplication()

    val currentLineCounter = AtomicInteger()

    fun nextCodeLine(code: String): ReplCodeLine = ReplCodeLine(currentLineCounter.getAndIncrement(), 0, code)

    val replCompiler: JvmReplCompiler by lazy {
        JvmReplCompiler(simpleScriptCompilationConfiguration)
    }

    val compiledEvaluator: ReplEvaluator by lazy {
        JvmReplEvaluator(simpleScriptEvaluationConfiguration)
    }

    val state by lazy {
        val stateLock = ReentrantReadWriteLock()
        AggregatedReplStageState(replCompiler.createState(stateLock), compiledEvaluator.createState(stateLock), stateLock)
    }

    override fun close() {
        state.dispose()
        resetApplicationToNull(application)
    }
}

private fun LegacyTestRepl.compileAndEval(codeLine: ReplCodeLine): Pair<ReplCompileResult, ReplEvalResult?> {

    val compRes = replCompiler.compile(state, codeLine)

    val evalRes = (compRes as? ReplCompileResult.CompiledClasses)?.let {

        compiledEvaluator.eval(state, it)
    }
    return compRes to evalRes
}

private fun assertEvalUnit(repl: LegacyTestRepl, line: String) {
    val compiledClasses = checkCompile(repl, line)

    val evalResult = repl.compiledEvaluator.eval(repl.state, compiledClasses!!)
    val unitResult = evalResult as? ReplEvalResult.UnitResult
    assertNotNull(unitResult, "Unexpected eval result: $evalResult")
}

private fun<R> assertEvalResult(repl: LegacyTestRepl, line: String, expectedResult: R) {
    val compiledClasses = checkCompile(repl, line)

    val evalResult = repl.compiledEvaluator.eval(repl.state, compiledClasses!!)
    val valueResult = evalResult as? ReplEvalResult.ValueResult
    assertNotNull(valueResult, "Unexpected eval result: $evalResult")
    assertEquals(expectedResult, valueResult.value)
}

private inline fun<reified R> assertEvalResultIs(repl: LegacyTestRepl, line: String) {
    val compiledClasses = checkCompile(repl, line)

    val evalResult = repl.compiledEvaluator.eval(repl.state, compiledClasses!!)
    val valueResult = evalResult as? ReplEvalResult.ValueResult
    assertNotNull(valueResult, "Unexpected eval result: $evalResult")
    assertTrue(valueResult.value is R)
}

private fun checkCompile(repl: LegacyTestRepl, line: String): ReplCompileResult.CompiledClasses? {
    val codeLine = repl.nextCodeLine(line)
    val compileResult = repl.replCompiler.compile(repl.state, codeLine)
    val compiledClasses = compileResult as? ReplCompileResult.CompiledClasses
    assertNotNull(compiledClasses, "Unexpected compile result: $compileResult")
    return compiledClasses
}
