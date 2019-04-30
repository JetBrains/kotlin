/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test

import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.cli.common.repl.BasicReplStageHistory
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.junit.Assert
import org.junit.Test
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.impl.KJvmReplCompilerImpl

class ReplTest : TestCase() {

    companion object {
        const val TEST_DATA_DIR = "libraries/scripting/jvm-host/testData"
    }

    @Test
    fun testCompileAndEval() {
        val out = captureOut {
            chechEvaluateInReplNoErrors(
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

    fun evaluateInRepl(
        compilationConfiguration: ScriptCompilationConfiguration,
        evaluationConfiguration: ScriptEvaluationConfiguration,
        snippets: Sequence<String>
    ): Sequence<ResultWithDiagnostics<EvaluationResult>> {
        val replCompilerProxy = KJvmReplCompilerImpl(defaultJvmScriptingHostConfiguration)
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
                    val snippetInstance = when (val retVal = it.returnValue) {
                        is ResultValue.Value -> retVal.scriptInstance
                        is ResultValue.UnitValue -> retVal.scriptInstance
                        else -> throw IllegalStateException("Expecting value with script instance, got $it")
                    }
                    currentEvalConfig = ScriptEvaluationConfiguration(currentEvalConfig) {
                        previousSnippets.append(snippetInstance)
                        jvm {
                            baseClassLoader(snippetInstance::class.java.classLoader)
                        }
                    }
                    it.asSuccess()
                }
        }
    }

    fun chechEvaluateInReplNoErrors(
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
                    val resVal = res.value.returnValue
                    if (resVal is ResultValue.Value && resVal.type.isNotBlank()) // TODO: the latter check is temporary while the result is used to return the instance too
                        Assert.assertEquals("#$index: Expected $expectedVal, got $resVal", expectedVal, resVal.value)
                    else
                        Assert.assertTrue("#$index: Expected $expectedVal, got Unit", expectedVal == null)
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