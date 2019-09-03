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
import org.junit.Assert
import org.junit.Test
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

class ReplTest : TestCase() {

    companion object {
        const val TEST_DATA_DIR = "libraries/scripting/jvm-host-test/testData"
    }

    @Test
    fun testCompileAndEval() {
        val out = captureOut {
            chechEvaluateInRepl(
                simpleScriptompilationConfiguration,
                simpleScriptEvaluationConfiguration,
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
        chechEvaluateInRepl(
            simpleScriptompilationConfiguration,
            simpleScriptEvaluationConfiguration,
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
        chechEvaluateInRepl(
            simpleScriptompilationConfiguration,
            simpleScriptEvaluationConfiguration,
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
        chechEvaluateInRepl(
            simpleScriptompilationConfiguration.with {
                implicitReceivers(TestReceiver::class)
            },
            simpleScriptEvaluationConfiguration.with {
                implicitReceivers(receiver)
            },
            sequenceOf(
                "val x = 4",
                "x + prop1",
                "res1 * 3"
            ),
            sequenceOf(null, 7, 21)
        )
    }

    @Test
    fun testEvalWithError() {
        chechEvaluateInRepl(
            simpleScriptompilationConfiguration,
            simpleScriptEvaluationConfiguration,
            sequenceOf(
                "throw RuntimeException(\"abc\")",
                "val x = 3",
                "x + 1"
            ),
            sequenceOf(RuntimeException("abc"), null, 4)
        )
    }

    fun evaluateInRepl(
        compilationConfiguration: ScriptCompilationConfiguration,
        evaluationConfiguration: ScriptEvaluationConfiguration,
        snippets: Sequence<String>
    ): Sequence<ResultWithDiagnostics<EvaluationResult>> {
        val replCompilerProxy =
            KJvmReplCompilerImpl(defaultJvmScriptingHostConfiguration)
        val compilationState = replCompilerProxy.createReplCompilationState(compilationConfiguration)
        val compilationHistory = BasicReplStageHistory<ScriptDescriptor>()
        val replEvaluator = BasicJvmScriptEvaluator()
        var currentEvalConfig = evaluationConfiguration
        return snippets.mapIndexed { snippetNo, snippetText ->
            val snippetSource = snippetText.toScriptSource("Line_$snippetNo.simplescript.kts")
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

    fun chechEvaluateInRepl(
        compilationConfiguration: ScriptCompilationConfiguration,
        evaluationConfiguration: ScriptEvaluationConfiguration,
        snippets: Sequence<String>,
        expected: Sequence<Any?>
    ) {
        val expectedIter = expected.iterator()
        evaluateInRepl(compilationConfiguration, evaluationConfiguration, snippets).forEachIndexed { index, res ->
            when (res) {
                is ResultWithDiagnostics.Failure -> Assert.fail("#$index: Expected result, got $res")
                is ResultWithDiagnostics.Success -> {
                    val expectedVal = expectedIter.next()
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
            }
        }
    }
}

@KotlinScript(fileExtension = "simplescript.kts")
abstract class SimpleScript

val simpleScriptompilationConfiguration = createJvmCompilationConfigurationFromTemplate<SimpleScript> {
    jvm {
        dependenciesFromCurrentContext(wholeClasspath = true)
    }
}

val simpleScriptEvaluationConfiguration = ScriptEvaluationConfiguration()

class TestReceiver(val prop1: Int = 3)
