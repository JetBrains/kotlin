/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test

import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.KJvmReplCompilerBase
import org.junit.Assert
import org.junit.Test
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.BasicJvmReplEvaluator
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

class ReplTest : TestCase() {

    @Test
    fun testCompileAndEval() {
        val out = captureOut {
            checkEvaluateInRepl(
                sequenceOf(
                    "val x = 3",
                    "x + 4",
                    "println(\"x = \$x\")"
                ),
                sequenceOf(null, 7, null)
            )
        }
        Assert.assertEquals("x = 3", out)
    }

    @Test
    fun testEvalWithResult() {
        checkEvaluateInRepl(
            sequenceOf(
                "val x = 5",
                "x + 6",
                "res1 * 2"
            ),
            sequenceOf(null, 11, 22)
        )
    }

    @Test
    fun testEvalWithIfResult() {
        checkEvaluateInRepl(
            sequenceOf(
                "val x = 5",
                "x + 6",
                "if (x < 10) res1 * 2 else x"
            ),
            sequenceOf(null, 11, 22)
        )
    }

    @Test
    fun testImplicitReceiver() {
        val receiver = TestReceiver()
        checkEvaluateInRepl(
            sequenceOf(
                "val x = 4",
                "x + prop1",
                "res1 * 3"
            ),
            sequenceOf(null, 7, 21),
            simpleScriptCompilationConfiguration.with {
                implicitReceivers(TestReceiver::class)
            },
            simpleScriptEvaluationConfiguration.with {
                implicitReceivers(receiver)
            }
        )
    }

    @Test
    fun testEvalWithError() {
        checkEvaluateInRepl(
            sequenceOf(
                "throw RuntimeException(\"abc\")",
                "val x = 3",
                "x + 1"
            ),
            sequenceOf(RuntimeException("abc"), null, 4)
        )
    }

    @Test
    fun testEvalWithErrorWithLocation() {
        checkEvaluateInReplDiags(
            sequenceOf(
                """
                    val foobar = 78
                    val foobaz = "dsdsda"
                    val ddd = ppp
                    val ooo = foobar
                """.trimIndent()
            ),
            sequenceOf(
                makeFailureResult(
                    "Unresolved reference: ppp", location = SourceCode.Location(
                        SourceCode.Position(3, 11), SourceCode.Position(3, 14)
                    )
                )
            )
        )
    }

    @Test
    fun testSyntaxErrors() {
        checkEvaluateInReplDiags(
            sequenceOf(
                "data class Q(val x: Int, val: String)",
                "fun g(): Unit { return }}",
                "fun f() : Int { return 1",
                "6*7"
            ),
            sequenceOf(
                makeFailureResult("Parameter name expected"),
                makeFailureResult("Unexpected symbol"),
                makeFailureResult("Expecting '}'"),
                42.asSuccess()
            )
        )
    }

    @Test
    fun testLongEval() {
        checkEvaluateInRepl(
            sequence {
                var count = 0
                while (true) {
                    val prev = if (count == 0) "0" else "obj${count - 1}.prop${count - 1} + $count"
                    yield("object obj$count { val prop$count = $prev }; $prev")
                    count++
                }
            },
            sequence {
                var acc = 0
                var count = 0
                while (true) {
                    yield(acc)
                    acc += ++count
                }
            },
            limit = 100
        )
    }

    companion object {
        private fun evaluateInRepl(
            snippets: Sequence<String>,
            compilationConfiguration: ScriptCompilationConfiguration = simpleScriptCompilationConfiguration,
            evaluationConfiguration: ScriptEvaluationConfiguration? = simpleScriptEvaluationConfiguration,
            limit: Int = 0
        ): Sequence<ResultWithDiagnostics<EvaluatedSnippet>> {
            val replCompiler = KJvmReplCompilerBase.create(defaultJvmScriptingHostConfiguration)
            val replEvaluator = BasicJvmReplEvaluator()
            val currentEvalConfig = evaluationConfiguration ?: ScriptEvaluationConfiguration()
            val snipetsLimited = if (limit == 0) snippets else snippets.take(limit)
            return snipetsLimited.mapIndexed { snippetNo, snippetText ->
                val snippetSource =
                    snippetText.toScriptSource("Line_$snippetNo.${compilationConfiguration[ScriptCompilationConfiguration.fileExtension]}")
                runBlocking { replCompiler.compile(snippetSource, compilationConfiguration) }
                    .onSuccess {
                        runBlocking { replEvaluator.eval(it, currentEvalConfig) }
                    }
                    .onSuccess {
                        it.get().asSuccess()
                    }
            }
        }

        fun checkEvaluateInReplDiags(
            snippets: Sequence<String>,
            expected: Sequence<ResultWithDiagnostics<Any?>>,
            compilationConfiguration: ScriptCompilationConfiguration = simpleScriptCompilationConfiguration,
            evaluationConfiguration: ScriptEvaluationConfiguration? = simpleScriptEvaluationConfiguration,
            limit: Int = 0
        ) {
            val expectedIter = (if (limit == 0) expected else expected.take(limit)).iterator()
            evaluateInRepl(snippets, compilationConfiguration, evaluationConfiguration, limit).forEachIndexed { index, res ->
                val expectedRes = expectedIter.next()
                when {
                    res is ResultWithDiagnostics.Failure && expectedRes is ResultWithDiagnostics.Failure -> {

                        val resReports = res.reports.filter {
                            it.code != ScriptDiagnostic.incompleteCode
                        }
                        Assert.assertTrue(
                            "#$index: Expected $expectedRes, got $res. Messages are different",
                            resReports.map { it.message } == expectedRes.reports.map { it.message }
                        )
                        Assert.assertTrue(
                            "#$index: Expected $expectedRes, got $res. Locations are different",
                            resReports.map { it.location }.zip(expectedRes.reports.map { it.location }).all {
                                it.second == null || it.second == it.first
                            }
                        )
                    }
                    res is ResultWithDiagnostics.Success && expectedRes is ResultWithDiagnostics.Success -> {
                        val expectedVal = expectedRes.value
                        val resVal = res.value.result
                        when (resVal) {
                            is ResultValue.Value -> Assert.assertEquals(
                                "#$index: Expected $expectedVal, got $resVal",
                                expectedVal,
                                resVal.value
                            )
                            is ResultValue.Unit -> Assert.assertNull("#$index: Expected $expectedVal, got Unit", expectedVal)
                            is ResultValue.Error -> Assert.assertTrue(
                                "#$index: Expected $expectedVal, got Error: ${resVal.error}",
                                expectedVal is Throwable && expectedVal.message == resVal.error?.message
                            )
                            else -> Assert.assertTrue("#$index: Expected $expectedVal, got unknown result $resVal", expectedVal == null)
                        }
                    }
                    else -> {
                        Assert.fail("#$index: Expected $expectedRes, got $res")
                    }
                }
            }
            if (expectedIter.hasNext()) {
                Assert.fail("Expected ${expectedIter.next()} got end of results stream")
            }
        }

        fun checkEvaluateInRepl(
            snippets: Sequence<String>,
            expected: Sequence<Any?>,
            compilationConfiguration: ScriptCompilationConfiguration = simpleScriptCompilationConfiguration,
            evaluationConfiguration: ScriptEvaluationConfiguration? = simpleScriptEvaluationConfiguration,
            limit: Int = 0
        ) = checkEvaluateInReplDiags(
            snippets, expected.map { ResultWithDiagnostics.Success(it) }, compilationConfiguration, evaluationConfiguration, limit
        )

        class TestReceiver(
            @Suppress("unused")
            val prop1: Int = 3
        )
    }
}
