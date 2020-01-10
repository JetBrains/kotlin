/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test

import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.cli.common.repl.BasicReplStageHistory
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.KJvmReplCompilerImpl
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.junit.Assert
import org.junit.Test
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.jvm

class ReplTest : TestCase() {

    companion object {
        const val TEST_DATA_DIR = "libraries/scripting/jvm-host-test/testData"
    }

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
            simpleScriptompilationConfiguration.with {
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
}

fun evaluateInRepl(
    snippets: Sequence<String>,
    compilationConfiguration: ScriptCompilationConfiguration = simpleScriptompilationConfiguration,
    evaluationConfiguration: ScriptEvaluationConfiguration? = simpleScriptEvaluationConfiguration
): Sequence<ResultWithDiagnostics<EvaluationResult>> {
    val replCompilerProxy =
        KJvmReplCompilerImpl(defaultJvmScriptingHostConfiguration)
    val compilationState = replCompilerProxy.createReplCompilationState(compilationConfiguration)
    val compilationHistory = BasicReplStageHistory<ScriptDescriptor>()
    val replEvaluator = BasicJvmScriptEvaluator()
    var currentEvalConfig = evaluationConfiguration ?: ScriptEvaluationConfiguration()
    return snippets.mapIndexed { snippetNo, snippetText ->
        val snippetSource = snippetText.toScriptSource("Line_$snippetNo.${compilationConfiguration[ScriptCompilationConfiguration.fileExtension]}")
        val snippetId = ReplSnippetIdImpl(snippetNo, 0, snippetSource)
        replCompilerProxy.compileReplSnippet(compilationState, snippetSource, snippetId, compilationHistory)
            .onSuccess {
                runBlocking {
                    replEvaluator(it, currentEvalConfig)
                }
            }
            .onSuccess {
                val snippetClass = it.returnValue.scriptClass
                currentEvalConfig = ScriptEvaluationConfiguration(currentEvalConfig) {
                    previousSnippets.append(it.returnValue.scriptInstance)
                    if (snippetClass != null) {
                        jvm {
                            baseClassLoader(snippetClass.java.classLoader)
                        }
                    }
                }
                it.asSuccess()
            }
    }
}

fun checkEvaluateInReplDiags(
    snippets: Sequence<String>,
    expected: Sequence<ResultWithDiagnostics<Any?>>,
    compilationConfiguration: ScriptCompilationConfiguration = simpleScriptompilationConfiguration,
    evaluationConfiguration: ScriptEvaluationConfiguration? = simpleScriptEvaluationConfiguration
) {
    val expectedIter = expected.iterator()
    evaluateInRepl(snippets, compilationConfiguration, evaluationConfiguration).forEachIndexed { index, res ->
        val expectedRes = expectedIter.next()
        when {
            res is ResultWithDiagnostics.Failure && expectedRes is ResultWithDiagnostics.Failure -> {
                Assert.assertTrue(
                    "#$index: Expected $expectedRes, got $res",
                    res.reports.map { it.message } == expectedRes.reports.map { it.message }
                )
            }
            res is ResultWithDiagnostics.Success && expectedRes is ResultWithDiagnostics.Success -> {
                val expectedVal = expectedRes.value
                when (val resVal = res.value.returnValue) {
                    is ResultValue.Value -> Assert.assertEquals(
                        "#$index: Expected $expectedVal, got $resVal",
                        expectedVal,
                        resVal.value
                    )
                    is ResultValue.Unit -> Assert.assertTrue("#$index: Expected $expectedVal, got Unit", expectedVal == null)
                    is ResultValue.Error -> Assert.assertTrue(
                        "#$index: Expected $expectedVal, got Error: ${resVal.error}",
                        expectedVal is Throwable && expectedVal.message == resVal.error.message
                    )
                    else -> Assert.assertTrue("#$index: Expected $expectedVal, got unknown result $resVal", expectedVal == null)
                }
            }
            else -> {
                Assert.fail("#$index: Expected $expectedRes, got $res")
            }
        }
    }
}

fun checkEvaluateInRepl(
    snippets: Sequence<String>,
    expected: Sequence<Any?>,
    compilationConfiguration: ScriptCompilationConfiguration = simpleScriptompilationConfiguration,
    evaluationConfiguration: ScriptEvaluationConfiguration? = simpleScriptEvaluationConfiguration
) = checkEvaluateInReplDiags(
    snippets, expected.map { ResultWithDiagnostics.Success(it) }, compilationConfiguration,
    evaluationConfiguration
)

class TestReceiver(val prop1: Int = 3)
