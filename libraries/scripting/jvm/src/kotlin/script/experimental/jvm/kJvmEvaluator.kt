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
import kotlin.script.experimental.jvm.util.JvmEvaluatedSnippet
import kotlin.script.experimental.util.LinkedSnippet
import kotlin.script.experimental.util.LinkedSnippetImpl
import kotlin.script.experimental.util.SnippetsHistory
import kotlin.script.experimental.util.add

class KJvmReplEvaluatorImpl(val scriptEvaluator: ScriptEvaluator = BasicJvmScriptEvaluator()) :
    ReplEvaluator<CompiledSnippet, KJvmEvaluatedSnippet> {
    override var lastEvaluatedSnippet: LinkedSnippetImpl<KJvmEvaluatedSnippet>? = null
        private set

    private val history = SnippetsHistory<KClass<*>?, Any?>()

    override suspend fun eval(
        snippet: LinkedSnippet<out CompiledSnippet>,
        configuration: ScriptEvaluationConfiguration
    ): ResultWithDiagnostics<LinkedSnippet<KJvmEvaluatedSnippet>> {

        val lastSnippetClass = history.lastItem()?.first
        val historyBeforeSnippet = history.items.map { it.second }
        val currentConfiguration = ScriptEvaluationConfiguration(configuration) {
            if (historyBeforeSnippet.isNotEmpty()) {
                previousSnippets.put(historyBeforeSnippet)
            }
            if (lastSnippetClass != null) {
                jvm {
                    baseClassLoader(lastSnippetClass.java.classLoader)
                }
            }
        }

        val snippetVal = snippet.get()
        val newEvalRes = when (val res = scriptEvaluator(snippetVal, currentConfiguration)) {
            is ResultWithDiagnostics.Success -> {
                when (val retVal = res.value.returnValue) {
                    is ResultValue.Error -> {
                        history.add(retVal.scriptClass, null)
                        val error = (retVal.error as? Throwable) ?: retVal.wrappingException
                        KJvmEvaluatedSnippet(snippetVal, currentConfiguration, retVal, error, null, false)
                    }
                    is ResultValue.Value -> {
                        history.add(retVal.scriptClass, retVal.scriptInstance)
                        KJvmEvaluatedSnippet(snippetVal, currentConfiguration, retVal, null, retVal.value, true)
                    }
                    is ResultValue.Unit -> {
                        history.add(retVal.scriptClass, retVal.scriptInstance)

                        KJvmEvaluatedSnippet(snippetVal, currentConfiguration, retVal, null, null, false)
                    }
                    else -> throw IllegalStateException("Unexpected snippet result value $retVal")
                }
            }
            else ->
                KJvmEvaluatedSnippet(
                    snippetVal, currentConfiguration, null,
                    res.reports.find { it.exception != null }?.exception, null, false
                )
        }

        val newNode = lastEvaluatedSnippet.add(newEvalRes)
        lastEvaluatedSnippet = newNode
        return newNode.asSuccess()
    }

}

class KJvmEvaluatedSnippet(
    override val compiledSnippet: CompiledSnippet,
    override val configuration: ScriptEvaluationConfiguration,
    resultValue: ResultValue?,
    override val error: Throwable?,
    override val result: Any?,
    override val hasResult: Boolean
) : JvmEvaluatedSnippet {
    override val snippetObject = resultValue?.scriptInstance
    val snippetClass = resultValue?.scriptClass
}
