/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test.k2repl

import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.script.experimental.api.EvaluatedSnippet
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.cellIndex
import kotlin.script.experimental.jvmhost.repl.k2.K2CompiledSnippet
import kotlin.script.experimental.jvmhost.repl.k2.K2ReplCompiler
import kotlin.script.experimental.jvmhost.repl.k2.K2ReplEvaluator
import kotlin.script.experimental.api.onSuccess
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.api.with
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.test.simpleScriptCompilationConfiguration
import kotlin.script.experimental.jvmhost.test.simpleScriptEvaluationConfiguration
import kotlin.script.experimental.jvmhost.test.throwOnFailure
import kotlin.script.experimental.jvmhost.test.withTempDir
import kotlin.script.experimental.util.LinkedSnippet
import kotlin.script.experimental.util.toLinkedSnippets
import kotlin.script.experimental.util.toList

/**
 * This class contains smoke tests for the K2 Repl implementation.
 *
 * These tests are written in a way where they manually define the expected output
 * of the compilers lowering phase when running on REPL scripts. This is because
 * the compiler doesn't yet support this.
 *
 * This also means that we are using a special version of [K2ReplCompiler] that just
 * compile normal Kotlin files rather than `kts` files.
 *
 * This setup allows us to iterate on how we want the lowering phase to behave as well
 * as iterate on the [kotlin.script.experimental.jvmhost.repl.k2.ReplState] API without
 * having compiler support
 */
class ReplSmokeTest : TestCase() {

    fun testNoOutput() {
        // Only used for documentation
        val snippets = listOf(
            """
                println("Hello, world!")
            """.trimIndent()
        )

        // Expected output of the compilers lowering phase
        val loweredSnippet = listOf(
            """
            package repl.snippet0
            import kotlin.script.experimental.jvmhost.repl.k2.ReplState
            import kotlin.script.experimental.jvmhost.repl.k2.ExecutableReplSnippet
            class Snippet0: ExecutableReplSnippet  {
                override suspend fun execute(replState: ReplState) {
                    println("Hello, world!")
                }
            }
            """.trimIndent()
        )
        evaluateInRepl(loweredSnippet).throwOnFailure()
    }

//    @Test
//    fun simpleValueOutput() {
//
//    }
//
//    @Test
//    fun suspendMethod() {
//
//    }
//
//    @Test
//    fun innerfunction() {
//
//    }
//
//    @Test
//    fun innerClass() {
//
//    }
//
//    @Test
//    fun innerEnum() {
//
//    }
//
//    @Test
//    fun callMethodInOtherSnippet() {
//
//    }
//
//    @Test
//    fun callClassInOtherSnippet() {
//
//    }
//
//    @Test
//    fun callEnumInOtherSnippet() {
//
//    }

    companion object {

        /**
         * @param loweredSnippet User code is split into statements, and each statement is compiled
         * as its own snippet, The list of strings provided here is the source code for the lowered
         * snippet.
         */
        private fun evaluateInRepl(
            loweredSnippet: List<String>,
            compilationConfiguration: ScriptCompilationConfiguration = simpleScriptCompilationConfiguration,
            evaluationConfiguration: ScriptEvaluationConfiguration? = simpleScriptEvaluationConfiguration,
            cellIndex: Int = 1,
        ): ResultWithDiagnostics<EvaluatedSnippet> {

            // In the final version, the compiler should automatically handle splitting the initial
            // code into multiple classes and compile all of them.
            // For now, we need to manually compile all the snippet classes and combine the final list
            var result: ResultWithDiagnostics<EvaluatedSnippet>? = null
            withTempDir("k2ReplTest") { tempDir ->
                val currentEvalConfig = evaluationConfiguration ?: ScriptEvaluationConfiguration()
                currentEvalConfig.with {
                    // Work-around for sending in the top-level cell index
                    this.cellIndex.put(cellIndex)
                }
                val compiledClasses = loweredSnippet.mapIndexed { snippetNo, snippetText ->
                    // Hard code extension for now so it is just simple files
                    val fileExt = "kt" // compilationConfiguration[ScriptCompilationConfiguration.fileExtension]
                    val snippetSource = snippetText.toScriptSource(
                        "Snippet_$snippetNo.$fileExt"
                    )
                    compileSnippet(snippetSource, tempDir)
                }.flatMap { it: LinkedSnippet<K2CompiledSnippet> ->
                    it.toList()
                }.toLinkedSnippets()

                runBlocking {
                    val replEvaluator = K2ReplEvaluator()
                    result = replEvaluator.eval(compiledClasses, currentEvalConfig)
                        .onSuccess { it.get().asSuccess() }
                        .throwOnFailure()
                }
            }
            return result!!
        }

        private fun compileSnippet(snippet: SourceCode, outputDir: File): LinkedSnippet<K2CompiledSnippet> {
            val compiler = K2ReplCompiler(outputDir)
            return runBlocking {
                compiler.compile(listOf(snippet), simpleScriptCompilationConfiguration).valueOrThrow()
            }
        }
    }
}
