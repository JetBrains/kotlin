/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.repl.k2

import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.KJvmEvaluatedSnippet
import kotlin.script.experimental.util.LinkedSnippet
import kotlin.script.experimental.util.LinkedSnippetImpl
import kotlin.script.experimental.util.add
import kotlin.script.experimental.util.toList

/**
 * Dummy class only used for experimentation, while the final API for K2 Repl is being
 * developed.
 */
class K2ReplEvaluator: ReplEvaluator<CompiledSnippet, EvaluatedSnippet> {
    override val lastEvaluatedSnippet: LinkedSnippet<EvaluatedSnippet>?
        get() = TODO("Not yet implemented")

    override suspend fun eval(
        snippet: LinkedSnippet<out CompiledSnippet>,
        configuration: ScriptEvaluationConfiguration,
    ): ResultWithDiagnostics<LinkedSnippet<EvaluatedSnippet>> {

        // Fow now, we are misusing the API and assume that the entire snippet
        // chain should be evaluated. This is not how the current
        // API is being used.
        val snippets = snippet.toList { it }

        // Construct a starting ReplState and run through the entire code
        // In a real use case, the ReplState should be part of a ReplSession
        // and preserved for the lifetime of the repl, so it can be parsed into
        // new code being run
        val state = ReplState()
        var snippetResults: LinkedSnippetImpl<EvaluatedSnippet>? = null
        snippets.forEachIndexed { i, el ->
            val snippetClass: KClass<*> = el.getClass(configuration).valueOrThrow()
            val snippetObj = snippetClass.createInstance() as ExecutableReplSnippet
            snippetObj.execute(state)

            // Return all intermediate results for now. We probably need to rethink this
            // Only the last snippet will have a return value due to each statement being
            // it its own snippet
            val result = if (i == snippets.lastIndex) {
                KJvmEvaluatedSnippet(
                    compiledSnippet = el,
                    configuration = configuration,
                    result = ResultValue.Unit(
                        scriptClass = snippetClass,
                        scriptInstance = snippetObj,
                    )
                )
            } else {
                val cellIndex: Int = configuration[ScriptEvaluationConfiguration.cellIndex]!!
                val resultValue = state.getOutput(cellIndex)
                KJvmEvaluatedSnippet(
                    compiledSnippet = el,
                    configuration = configuration,
                    result = resultValue ?: ResultValue.Unit(
                        scriptClass = snippetClass,
                        scriptInstance = snippetObj,
                    )
                )
            }
            snippetResults = snippetResults.add(result)
        }
        return snippetResults!!.asSuccess()
    }
}
