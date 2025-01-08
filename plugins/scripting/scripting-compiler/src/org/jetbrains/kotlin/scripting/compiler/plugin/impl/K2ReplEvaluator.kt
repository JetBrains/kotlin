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
        val compiledSnippet = snippet.get() as KJvmCompiledScript

        val currentConfiguration = lastEvaluatedSnippet?.let {
            configuration.with {
                jvm {
                    lastSnippetClassLoader(it.get().result.scriptInstance?.javaClass?.classLoader)
                }
            }
        } ?: configuration

        return compiledSnippet.getClass(currentConfiguration).onSuccess { snippetClass ->
            evalSnippet(snippetClass).let { evaluationResult ->
                LinkedSnippetImpl(
                    KJvmEvaluatedSnippet(compiledSnippet, currentConfiguration, evaluationResult),
                    lastEvaluatedSnippet
                ).also {
                    lastEvaluatedSnippet = it
                }
            }.asSuccess()
        }
    }

    private fun evalSnippet(snippetClass: KClass<*>): ResultValue {
        val evalFunName = REPL_SNIPPET_EVAL_FUN_NAME.asString()
        val eval = snippetClass.java.methods.find { it.name == evalFunName }!!

        val snippet = snippetClass.java.getField("INSTANCE").get(null)

        return try {
            val result = eval.invoke(snippet)

            // TODO: consider generalizing snippet and script evaluation result handling (KT-74300)
            // (currently scripts are using result fields while snippets - return value from eval fun, but approach is stricter in
            // scripts, because the result field, with name and type, is extracted from IR, while here only reflection is used).
            if (eval.returnType.name == "void") {
                ResultValue.Unit(snippetClass, snippet)
            } else {
                ResultValue.Value("", result, eval.returnType.name, snippetClass, snippet)
            }
        } catch (e: Throwable) {
            ResultValue.Error((e as? InvocationTargetException)?.targetException ?: e, e, snippetClass)
        }
    }
}

