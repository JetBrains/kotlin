/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.test

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.scripting.compiler.plugin.SCRIPT_TEST_BASE_COMPILER_ARGUMENTS_PROPERTY
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplCompiler
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplEvaluator
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.withMessageCollectorAndDisposable
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.KJvmEvaluatedSnippet
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.util.LinkedSnippet
import kotlin.test.*

class ReplReceiver1 {
    val ok = "OK"
}

class CustomK2ReplTest {

    @Test
    fun testSimple() {
        evalAndCheckSnippets(
            sequenceOf(
                "val x = 3",
                "x + 4",
                "x"
            ),
            sequenceOf(
                null,
                7,
                3
            )
        )
    }

    @Test
    fun testWithImplicitReceiver() {
        evalAndCheckSnippets(
            sequenceOf("val x = ok", "ok", "x",),
            sequenceOf(null, "OK", "OK"),
            baseCompilationConfiguration.with {
                implicitReceivers(ReplReceiver1::class)
            },
            baseEvaluationConfiguration.with {
                implicitReceivers(ReplReceiver1())
            }
        )
    }

    @Test
    fun testWithImplicitReceiverWithShadowing() {
        evalAndCheckSnippets(
            sequenceOf("val ok = 42", "ok",),
            sequenceOf(null, 42),
            baseCompilationConfiguration.with {
                implicitReceivers(ReplReceiver1::class)
            },
            baseEvaluationConfiguration.with {
                implicitReceivers(ReplReceiver1())
            }
        )
    }
}

private val baseCompilationConfiguration: ScriptCompilationConfiguration =
    ScriptCompilationConfiguration {
        val classpath = System.getProperty("kotlin.test.script.classpath")?.split(File.pathSeparator)
            ?.mapNotNull { File(it).takeIf { file -> file.exists() } }.orEmpty()
        updateClasspath(classpath + ForTestCompileRuntime.runtimeJarForTests())
        compilerOptions("-Xrender-internal-diagnostic-names=true")
    }

private val baseEvaluationConfiguration: ScriptEvaluationConfiguration = ScriptEvaluationConfiguration {}

private fun compileAndEvalSnippets(
    snippets: Sequence<String>,
    compilationConfiguration: ScriptCompilationConfiguration,
    evaluationConfiguration: ScriptEvaluationConfiguration
): ResultWithDiagnostics<List<LinkedSnippet<KJvmEvaluatedSnippet>>> =
    withMessageCollectorAndDisposable { messageCollector, disposable ->
        val compiler = K2ReplCompiler(K2ReplCompiler.createCompilationState(messageCollector, disposable, compilationConfiguration))
        val evaluator = K2ReplEvaluator()
        val filenameExtension = compilationConfiguration[ScriptCompilationConfiguration.fileExtension] ?: "repl.kts"
        var snippetNo = 1
        @Suppress("DEPRECATION_ERROR")
        internalScriptingRunSuspend {
            snippets.asIterable().mapSuccess { snippet ->
                compiler.compile(snippet.toScriptSource("s${snippetNo++}.$filenameExtension")).onSuccess {
                    evaluator.eval(it, evaluationConfiguration)
                }
            }
        }
    }

private fun checkEvaluatedSnippets(
    expectedResultVals: Sequence<Any?>,
    evaluationResults: ResultWithDiagnostics<List<LinkedSnippet<KJvmEvaluatedSnippet>>>
) {
    val expectedIter = expectedResultVals.iterator()
    val successResults = evaluationResults.valueOr {
        fail("Evaluation failed:\n  ${it.reports.joinToString("\n  ") { it.message }}")
        return
    }
    for (res in successResults) {
        val expectedVal = expectedIter.next()
        when (val resVal = res.get().result) {
            is ResultValue.Unit -> assertTrue(expectedVal == null, "Unexpected evaluation result: Unit")
            is ResultValue.Error -> fail("Unexpected evaluation result: runtime error: ${resVal.error.message}")
            is ResultValue.Value -> assertTrue(expectedVal == resVal.value, "Unexpected evaluation result: ${resVal.value}")
            is ResultValue.NotEvaluated -> fail("Unexpected evaluation result: NotEvaluated")
        }
    }
}

private fun evalAndCheckSnippets(
    snippets: Sequence<String>,
    expectedResultVals: Sequence<Any?>,
    compilationConfiguration: ScriptCompilationConfiguration = baseCompilationConfiguration,
    evaluationConfiguration: ScriptEvaluationConfiguration = baseEvaluationConfiguration
) {
    // this is K2-only tests
    if (System.getProperty(SCRIPT_TEST_BASE_COMPILER_ARGUMENTS_PROPERTY)?.contains("-language-version 1.9") == true) return

    val evaluationResults = compileAndEvalSnippets(snippets, compilationConfiguration, evaluationConfiguration)
    checkEvaluatedSnippets(expectedResultVals, evaluationResults)
}
