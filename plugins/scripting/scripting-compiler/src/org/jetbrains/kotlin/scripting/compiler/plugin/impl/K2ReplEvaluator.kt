/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.scripting.compiler.plugin.irLowerings.REPL_SNIPPET_EVAL_FUN_NAME
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.KJvmEvaluatedSnippet
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.lastSnippetClassLoader
import kotlin.script.experimental.util.LinkedSnippet
import kotlin.script.experimental.util.LinkedSnippetImpl

class K2ReplEvaluator : ReplEvaluator<CompiledSnippet, KJvmEvaluatedSnippet> {

    override var lastEvaluatedSnippet: LinkedSnippetImpl<KJvmEvaluatedSnippet>? = null
        private set

    override suspend fun eval(
        snippet: LinkedSnippet<out CompiledSnippet>,
        configuration: ScriptEvaluationConfiguration,
    ): ResultWithDiagnostics<LinkedSnippet<KJvmEvaluatedSnippet>> {
        // Walk the chain back to find compiled snippets not yet evaluated. Required for
        // synthetic snippets produced by `prependSyntheticSnippets`: they are appended to the
        // compiler's LinkedSnippet chain together with the user snippet, but only the tail node
        // is passed here. Without walking, their classes (and any `ReplState` object they own,
        // and side effects like `val bindings = getBindings(...)`) would never be loaded/run.
        val pending = mutableListOf<LinkedSnippet<out CompiledSnippet>>()
        var current: LinkedSnippet<out CompiledSnippet>? = snippet
        val lastEvalCompiled = lastEvaluatedSnippet?.get()?.compiledSnippet
        while (current != null && current.get() !== lastEvalCompiled) {
            pending.add(0, current)
            current = current.previous
        }
        if (pending.isEmpty()) {
            return lastEvaluatedSnippet?.asSuccess()
                ?: ResultWithDiagnostics.Failure("No snippets to evaluate".asErrorDiagnostics())
        }

        for (pendingSnippet in pending) {
            val compiledSnippet = pendingSnippet.get() as KJvmCompiledScript

            val currentConfiguration = lastEvaluatedSnippet?.let {
                configuration.with {
                    jvm {
                        (it.get().result.scriptClass?.java ?: it.get().result.scriptInstance?.javaClass)?.let {
                            lastSnippetClassLoader(it.classLoader)
                        }
                    }
                }
            } ?: configuration

            val refinedEvalConfiguration =
                currentConfiguration.with {
                    compilationConfiguration(compiledSnippet.compilationConfiguration)
                }.refineBeforeEvaluation(compiledSnippet).valueOr {
                    return ResultWithDiagnostics.Failure(it.reports)
                }

            val classResult = compiledSnippet.getClass(refinedEvalConfiguration)
            if (classResult is ResultWithDiagnostics.Failure) return classResult
            val snippetClass = (classResult as ResultWithDiagnostics.Success).value
            val evaluationResult = evalSnippet(compiledSnippet, snippetClass, refinedEvalConfiguration)
            lastEvaluatedSnippet = LinkedSnippetImpl(
                KJvmEvaluatedSnippet(compiledSnippet, refinedEvalConfiguration, evaluationResult),
                lastEvaluatedSnippet
            )
        }
        return lastEvaluatedSnippet!!.asSuccess()
    }

    private fun evalSnippet(compiledSnippet: KJvmCompiledScript, snippetClass: KClass<*>, configuration: ScriptEvaluationConfiguration): ResultValue {
        val evalFunName = REPL_SNIPPET_EVAL_FUN_NAME.asString()
        val eval = snippetClass.java.methods.find { it.name == evalFunName }!!

        val snippet = snippetClass.java.getField("INSTANCE").get(null)

        val args = mutableListOf<Any?>()

        configuration[ScriptEvaluationConfiguration.implicitReceivers]?.let {
            args.addAll(it)
        }

        return try {
            eval.invoke(snippet, *args.toTypedArray())

            compiledSnippet.resultField?.let { [resultFieldName, resultType] ->
                // The result field is sometimes recorded in the compiled snippet metadata but the
                // corresponding bytecode field is not actually emitted (e.g. when the FIR transformation
                // turns the snippet body into a statement-only form). Fall back to Unit instead of crashing.
                val resultField = runCatching { snippetClass.java.getDeclaredField(resultFieldName) }.getOrNull()
                when {
                    resultField == null -> ResultValue.Unit(snippetClass, snippet)
                    resultType.typeName == "kotlin.Unit" -> ResultValue.Unit(snippetClass, snippet)
                    else -> ResultValue.Value(resultFieldName, resultField.get(snippet), resultType.typeName, snippetClass, snippet)
                }
            } ?: ResultValue.Unit(snippetClass, snippet)
        } catch (e: Throwable) {
            ResultValue.Error((e as? InvocationTargetException)?.targetException ?: e, e, snippetClass)
        }
    }
}

