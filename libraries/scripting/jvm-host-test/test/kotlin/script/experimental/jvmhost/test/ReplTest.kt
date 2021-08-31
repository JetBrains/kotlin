/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test

import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.KJvmReplCompilerBase
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.ReplCodeAnalyzerBase
import org.junit.Assert
import org.junit.Test
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.BasicJvmReplEvaluator
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.classpathFromClass
import kotlin.script.experimental.jvmhost.createJvmScriptDefinitionFromTemplate
import kotlin.script.templates.standard.ScriptTemplateWithArgs

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
    fun testEvalWithExceptionWithCause() {
        checkEvaluateInRepl(
            sequenceOf(
                """
                    try {
                        throw Exception("Error!")
                    } catch (e: Exception) {
                        throw Exception("Oh no", e)
                    }
                """.trimIndent()
            ),
            sequenceOf(Exception("Oh no", Exception("Error!")))
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
    // TODO: make it covering more cases
    fun testIrReceiverOvewrite() {
        checkEvaluateInRepl(
            sequenceOf(
                "fun f(a: String) = a",
                "f(\"x\")"
            ),
            sequenceOf(
                null,
                "x"
            )
        )
    }

    @Test
    fun testNoEvaluationError() {
        checkEvaluateInReplDiags(
            sequenceOf(
                """
                    fun stack(vararg tup: Int): Int = tup.sum()
                    val X = 1
                    val x = stack(1, X)
                """.trimIndent(),
                "val y = 42"
            ),
            sequenceOf(
                ResultValue.NotEvaluated.asSuccess(
                    listOf(
                        ScriptDiagnostic(
                            ScriptDiagnostic.unspecifiedError,
                            "Unable to instantiate class Line_0_simplescript: java.lang.ClassFormatError: " +
                                    "Duplicate method name \"getX\" with signature \"()I\" in class file Line_0_simplescript"
                        )
                    )
                ),
                makeFailureResult("Snippet cannot be evaluated due to history mismatch")
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

    @Test
    fun testAddNewAnnotationHandler() {
        val replCompiler = KJvmReplCompilerBase<ReplCodeAnalyzerBase>()
        val replEvaluator = BasicJvmReplEvaluator()
        val compilationConfiguration = ScriptCompilationConfiguration().with {
            updateClasspath(classpathFromClass<NewAnn>())
        }
        val evaluationConfiguration = ScriptEvaluationConfiguration()

        val res0 = runBlocking {
            replCompiler.compile("1".toScriptSource("Line_0.kts"), compilationConfiguration).onSuccess {
                replEvaluator.eval(it, evaluationConfiguration)
            }
        }
        assertTrue(
            "Expecting 1 got $res0",
            res0 is ResultWithDiagnostics.Success && (res0.value.get().result as ResultValue.Value).value == 1
        )

        var handlerInvoked = false

        val compilationConfiguration2 = compilationConfiguration.with {
            refineConfiguration {
//                defaultImports(NewAnn::class) // TODO: fix support for default imports
                onAnnotations<NewAnn> {
                    handlerInvoked = true
                    it.compilationConfiguration.asSuccess()
                }
            }
        }

        val res1 = runBlocking {
            replCompiler.compile(
                "@file:kotlin.script.experimental.jvmhost.test.NewAnn()\n2".toScriptSource("Line_1.kts"),
                compilationConfiguration2
            ).onSuccess {
                replEvaluator.eval(it, evaluationConfiguration)
            }
        }
        assertTrue(
            "Expecting 2 got $res1",
            res1 is ResultWithDiagnostics.Success && (res1.value.get().result as ResultValue.Value).value == 2
        )

        assertTrue("Refinement handler on annotation is not invoked", handlerInvoked)
    }

    @Test
    fun testDefinitionWithConstructorArgs() {
        val scriptDef = createJvmScriptDefinitionFromTemplate<ScriptTemplateWithArgs>(
            evaluation = {
                constructorArgs(arrayOf("a"))
            }
        )

        checkEvaluateInRepl(
            sequenceOf(
                "args[0]",
                "res0+args[0]",
                "res1+args[0]"
            ),
            sequenceOf("a", "aa", "aaa"),
            scriptDef.compilationConfiguration,
            scriptDef.evaluationConfiguration
        )
    }

    companion object {
        private fun evaluateInRepl(
            snippets: Sequence<String>,
            compilationConfiguration: ScriptCompilationConfiguration = simpleScriptCompilationConfiguration,
            evaluationConfiguration: ScriptEvaluationConfiguration? = simpleScriptEvaluationConfiguration,
            limit: Int = 0
        ): Sequence<ResultWithDiagnostics<EvaluatedSnippet>> {
            val replCompiler = KJvmReplCompilerBase<ReplCodeAnalyzerBase>()
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
            limit: Int = 0,
            ignoreDiagnostics: Boolean = false
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
                        val actualVal = res.value.result
                        when (actualVal) {
                            is ResultValue.Value -> Assert.assertEquals(
                                "#$index: Expected $expectedVal, got $actualVal",
                                expectedVal,
                                actualVal.value
                            )
                            is ResultValue.Unit -> Assert.assertNull("#$index: Expected $expectedVal, got Unit", expectedVal)
                            is ResultValue.Error -> Assert.assertTrue(
                                "#$index: Expected $expectedVal, got Error: ${actualVal.error}",
                                expectedVal is Throwable && expectedVal.message == actualVal.error.message
                                        && expectedVal.cause?.message == actualVal.error.cause?.message
                            )
                            is ResultValue.NotEvaluated -> Assert.assertEquals(
                                "#$index: Expected $expectedVal, got NotEvaluated",
                                expectedVal, actualVal
                            )
                            else -> Assert.assertTrue("#$index: Expected $expectedVal, got unknown result $actualVal", expectedVal == null)
                        }
                        if (!ignoreDiagnostics) {
                            val expectedDiag = expectedRes.reports
                            val actualDiag = res.reports
                            Assert.assertEquals(
                                "Diagnostics should be same",
                                expectedDiag.map { it.toString() },
                                actualDiag.map { it.toString() }
                            )
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
            snippets, expected.map { ResultWithDiagnostics.Success(it) }, compilationConfiguration, evaluationConfiguration, limit, true
        )

        class TestReceiver(
            @Suppress("unused")
            val prop1: Int = 3
        )
    }
}

@Target(AnnotationTarget.FILE)
annotation class NewAnn
