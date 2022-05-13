/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.util.SnippetsHistory
import kotlin.script.experimental.util.LinkedSnippet
import kotlin.script.experimental.util.LinkedSnippetImpl
import kotlin.script.experimental.util.add

class BasicJvmReplEvaluator(val scriptEvaluator: ScriptEvaluator = BasicJvmScriptEvaluator()) :
    ReplEvaluator<CompiledSnippet, KJvmEvaluatedSnippet> {
    override var lastEvaluatedSnippet: LinkedSnippetImpl<KJvmEvaluatedSnippet>? = null
        private set

    private val history = SnippetsHistory<KClass<*>?, Any?>()

    override suspend fun eval(
        snippet: LinkedSnippet<out CompiledSnippet>,
        configuration: ScriptEvaluationConfiguration
    ): ResultWithDiagnostics<LinkedSnippet<KJvmEvaluatedSnippet>> {

        if (!verifyHistoryConsistency(snippet))
            return ResultWithDiagnostics.Failure(
                ScriptDiagnostic(ScriptDiagnostic.unspecifiedError, "Snippet cannot be evaluated due to history mismatch")
            )

        val lastSnippetClass = history.lastItem()?.first
        val historyBeforeSnippet = history.items.map { it.second }
        val currentConfiguration = ScriptEvaluationConfiguration(configuration) {
            previousSnippets.put(historyBeforeSnippet)
            if (lastSnippetClass != null) {
                jvm {
                    lastSnippetClassLoader(lastSnippetClass.java.classLoader)
                }
            }
        }

        val snippetVal = snippet.get()
        val evalRes = scriptEvaluator(snippetVal, currentConfiguration)
        val newEvalRes = when (evalRes) {
            is ResultWithDiagnostics.Success -> {
                val retVal = evalRes.value.returnValue
                when (retVal) {
                    is ResultValue.Error -> history.add(retVal.scriptClass, null)
                    is ResultValue.Value, is ResultValue.Unit -> history.add(retVal.scriptClass, retVal.scriptInstance)
                    is ResultValue.NotEvaluated -> {}
                }
                KJvmEvaluatedSnippet(snippetVal, currentConfiguration, retVal)
            }
            else -> {
                val firstError = evalRes.reports.find { it.isError() }
                KJvmEvaluatedSnippet(
                    snippetVal, currentConfiguration,
                    firstError?.exception?.let { ResultValue.Error(it) } ?: ResultValue.NotEvaluated
                )
            }
        }

        val newNode = lastEvaluatedSnippet.add(newEvalRes)
        lastEvaluatedSnippet = newNode
        return newNode.asSuccess(evalRes.reports)
    }

    private fun verifyHistoryConsistency(compiledSnippet: LinkedSnippet<out CompiledSnippet>): Boolean {
        var compiled = compiledSnippet.previous
        var evaluated = lastEvaluatedSnippet
        while (compiled != null && evaluated != null) {
            val evaluatedVal = evaluated.get()
            if (evaluatedVal.compiledSnippet !== compiled.get())
                return false
            if (evaluatedVal.result.scriptClass == null)
                return false
            compiled = compiled.previous
            evaluated = evaluated.previous
        }
        return compiled == null && evaluated == null
    }
}

class KJvmEvaluatedSnippet(
    override val compiledSnippet: CompiledSnippet,
    override val configuration: ScriptEvaluationConfiguration,
    override val result: ResultValue
) : EvaluatedSnippet
