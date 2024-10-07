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

        // Fow now, we are assuming that `snippet` always only contain one element.
        // Unclear if we can keep this invariant going forward
        val snippetElement = snippet.get()
        var snippetResults: LinkedSnippetImpl<EvaluatedSnippet>? = null

        // Construct a starting ReplState and run through the entire code
        // In a real use case, the ReplState should be part of a ReplSession
        // and preserved for the lifetime of the repl, so it can be parsed into
        // new code being run
        val state = ReplState()
        val snippetClass: KClass<*> = snippetElement.getClass(configuration).valueOrThrow()
        val snippetObj = snippetClass.createInstance() as ExecutableReplSnippet
        try {
            snippetObj.evaluate(state)
        } catch (e: Throwable) {
            state.setErrorOutput(ResultValue.Error(error = e, scriptClass = snippetClass::class))
        }

        val resultValue = state.getLastOutput()
        val snippetResult = KJvmEvaluatedSnippet(
            compiledSnippet = snippetElement,
            configuration = configuration,
            result = resultValue
        )
        snippetResults = snippetResults.add(snippetResult)
        return snippetResults.asSuccess()
    }
}
