/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test

import com.intellij.openapi.application.ApplicationManager
import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase.resetApplicationToNull
import java.io.Closeable
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.script.experimental.jvmhost.repl.JvmReplCompiler
import kotlin.script.experimental.jvmhost.repl.JvmReplEvaluator

// Adapted form GenericReplTest

// Artificial split into several testsuites, to speed up parallel testing
class LegacyReplTest : TestCase() {
    fun testReplBasics() {
        LegacyTestRepl().use { repl ->
            val res1 = repl.replCompiler.check(repl.state, ReplCodeLine(0, 0, "val x ="))
            TestCase.assertTrue("Unexpected check results: $res1", res1 is ReplCheckResult.Incomplete)

            assertEvalResult(repl, "val l1 = listOf(1 + 2)\nl1.first()", 3)

            assertEvalUnit(repl, "val x = 5")

            assertEvalResult(repl, "x + 2", 7)
        }
    }

    fun testReplErrors() {
        LegacyTestRepl().use { repl ->
            repl.compileAndEval(repl.nextCodeLine("val x = 10"))

            val res = repl.compileAndEval(repl.nextCodeLine("java.util.fish"))
            TestCase.assertTrue("Expected compile error", res.first is ReplCompileResult.Error)

            val result = repl.compileAndEval(repl.nextCodeLine("x"))
            assertEquals(res.second.toString(), 10, (result.second as? ReplEvalResult.ValueResult)?.value)
        }
    }

    fun testReplSyntaxErrorsChecked() {
        LegacyTestRepl().use { repl ->
            val res = repl.compileAndEval(repl.nextCodeLine("data class Q(val x: Int, val: String)"))
            TestCase.assertTrue("Expected compile error", res.first is ReplCompileResult.Error)
        }
    }

    fun testReplCodeFormat() {
        LegacyTestRepl().use { repl ->
            val codeLine0 = ReplCodeLine(0, 0, "val l1 = 1\r\nl1\r\n")
            val res0 = repl.replCompiler.check(repl.state, codeLine0)
            val res0c = res0 as? ReplCheckResult.Ok
            TestCase.assertNotNull("Unexpected compile result: $res0", res0c)
        }
    }

    fun testRepPackage() {
        LegacyTestRepl().use { repl ->
            assertEvalResult(repl, "package mypackage\n\nval x = 1\nx+2", 3)

            assertEvalResult(repl, "x+4", 5)
        }
    }

    fun testReplResultFieldWithFunction() {
        LegacyTestRepl().use { repl ->
            assertEvalResultIs<Function0<Int>>(repl, "{ 1 + 2 }")
            assertEvalResultIs<Function0<Int>>(repl, "res0")
            assertEvalResult(repl, "res0()", 3)
        }
    }

    fun testReplResultField() {
        LegacyTestRepl().use { repl ->
            assertEvalResult(repl, "5 * 4", 20)
            assertEvalResult(repl, "res0 + 3", 23)
        }
    }
}

// Artificial split into several testsuites, to speed up parallel testing
class LegacyReplTestLong1 : TestCase() {

    fun test256Evals() {
        LegacyTestRepl().use { repl ->
            repl.compileAndEval(ReplCodeLine(0, 0, "val x0 = 0"))

            val evals = 256
            for (i in 1..evals) {
                repl.compileAndEval(ReplCodeLine(i, 0, "val x$i = x${i-1} + 1"))
            }

            val res = repl.compileAndEval(ReplCodeLine(evals + 1, 0, "x$evals"))
            assertEquals(res.second.toString(), evals, (res.second as? ReplEvalResult.ValueResult)?.value)
        }
    }
}

// Artificial split into several testsuites, to speed up parallel testing
class LegacyReplTestLong2 : TestCase() {

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
        JvmReplCompiler(simpleScriptompilationConfiguration)
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
    TestCase.assertNotNull("Unexpected eval result: $evalResult", unitResult)
}

private fun<R> assertEvalResult(repl: LegacyTestRepl, line: String, expectedResult: R) {
    val compiledClasses = checkCompile(repl, line)

    val evalResult = repl.compiledEvaluator.eval(repl.state, compiledClasses!!)
    val valueResult = evalResult as? ReplEvalResult.ValueResult
    TestCase.assertNotNull("Unexpected eval result: $evalResult", valueResult)
    TestCase.assertEquals(expectedResult, valueResult!!.value)
}

private inline fun<reified R> assertEvalResultIs(repl: LegacyTestRepl, line: String) {
    val compiledClasses = checkCompile(repl, line)

    val evalResult = repl.compiledEvaluator.eval(repl.state, compiledClasses!!)
    val valueResult = evalResult as? ReplEvalResult.ValueResult
    TestCase.assertNotNull("Unexpected eval result: $evalResult", valueResult)
    TestCase.assertTrue(valueResult!!.value is R)
}

private fun checkCompile(repl: LegacyTestRepl, line: String): ReplCompileResult.CompiledClasses? {
    val codeLine = repl.nextCodeLine(line)
    val compileResult = repl.replCompiler.compile(repl.state, codeLine)
    val compiledClasses = compileResult as? ReplCompileResult.CompiledClasses
    TestCase.assertNotNull("Unexpected compile result: $compileResult", compiledClasses)
    return compiledClasses
}
