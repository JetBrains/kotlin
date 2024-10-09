/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test.k2repl

import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.repl.k2.K2CompiledSnippet
import kotlin.script.experimental.jvmhost.repl.k2.K2ReplCompiler
import kotlin.script.experimental.jvmhost.repl.k2.K2ReplEvaluator
import kotlin.script.experimental.jvmhost.test.simpleScriptCompilationConfiguration
import kotlin.script.experimental.jvmhost.test.simpleScriptEvaluationConfiguration
import kotlin.script.experimental.jvmhost.test.throwOnFailure
import kotlin.script.experimental.jvmhost.test.withTempDir
import kotlin.script.experimental.util.*

/**
 * @param loweredCells A list of user cells to evaluate. The input should be in the lowered
 * form, i.e., the expected output of the compiler once that part is done.
 *
 * The results of all evaluated cells are returned
 */
fun evaluateInRepl(
    loweredSnippets: List<String>,
    compilationConfiguration: ScriptCompilationConfiguration = simpleScriptCompilationConfiguration,
    evaluationConfiguration: ScriptEvaluationConfiguration? = simpleScriptEvaluationConfiguration,
): List<ResultWithDiagnostics<EvaluatedSnippet>> {
    var lastCompiledSnippet: LinkedSnippetImpl<K2CompiledSnippet>? = null
    val outputs = mutableListOf<ResultWithDiagnostics<EvaluatedSnippet>>()
    val replEvaluator = K2ReplEvaluator()
    withTempDir("k2ReplTest") { tempDir ->
        loweredSnippets.toList().forEachIndexed { cellId: Int, snippet: String ->
            // In the final version, the compiler should automatically handle splitting the initial
            // code into multiple classes and compile all of them.
            // For now, we need to manually compile all the snippet classes and combine the final list
            val currentEvalConfig = evaluationConfiguration ?: ScriptEvaluationConfiguration()
            currentEvalConfig.with {
                // Work-around for sending in the top-level cell index
                this.cellIndex.put(cellId)
            }
            val compiledClasses = snippet.let {
                // Hard code extension for now so it is just simple files
                val fileExt = "kt" // compilationConfiguration[ScriptCompilationConfiguration.fileExtension]
                val snippetSource = snippet.toScriptSource(
                    "Snippet_$cellId.$fileExt"
                )
                compileSnippet(snippetSource, tempDir).toList().toLinkedSnippets()
            }
            lastCompiledSnippet = lastCompiledSnippet.add(compiledClasses.get())
            runBlocking {
                val code = lastCompiledSnippet as LinkedSnippet<K2CompiledSnippet>
                val result = replEvaluator.eval(code, currentEvalConfig)
                    .onSuccess { it.get().asSuccess() }
                    .throwOnFailure()
                outputs.add(result)
            }
        }
    }

    return outputs
}

private fun compileSnippet(snippet: SourceCode, outputDir: File): LinkedSnippet<K2CompiledSnippet> {
    val compiler = K2ReplCompiler(outputDir)
    return runBlocking {
        compiler.compile(listOf(snippet), simpleScriptCompilationConfiguration).valueOrThrow()
    }
}