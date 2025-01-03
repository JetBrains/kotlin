/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass
import kotlin.reflect.full.memberFunctions
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.KJvmEvaluatedSnippet
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.util.LinkedSnippet
import kotlin.script.experimental.util.LinkedSnippetImpl

class K2ReplEvaluator: ReplEvaluator<CompiledSnippet, KJvmEvaluatedSnippet> {

    override var lastEvaluatedSnippet: LinkedSnippetImpl<KJvmEvaluatedSnippet>? = null
        private set

    override suspend fun eval(
        snippet: LinkedSnippet<out CompiledSnippet>,
        configuration: ScriptEvaluationConfiguration,
    ): ResultWithDiagnostics<LinkedSnippet<KJvmEvaluatedSnippet>> {
        val compiledSnippet = snippet.get() as KJvmCompiledScript

        return compiledSnippet.getClass(configuration).onSuccess { snippetClass ->
            evalSnippet(snippetClass, compiledSnippet.resultField).let { evaluationResult ->
                LinkedSnippetImpl(KJvmEvaluatedSnippet(compiledSnippet, configuration, evaluationResult), lastEvaluatedSnippet).also {
                    lastEvaluatedSnippet = it
                }
            }.asSuccess()
        }
    }

    private fun evalSnippet(snippetClass: KClass<*>, resultFieldData: Pair<String, KotlinType>?): ResultValue {
        val eval = snippetClass.memberFunctions.find { it.name == "eval" }!!

        val snippet = snippetClass.objectInstance!!

        return try {
            eval.call(snippet)

            val resultField = resultFieldData?.let {
                try {
                    snippetClass.java.getDeclaredField(it.first).apply { isAccessible = true }
                } catch (_: NoSuchFieldException) {
                    null
                }
            }
            if (resultField == null) {
                ResultValue.Unit(snippetClass, snippet)
            } else {
                val resultValue = resultField.get(snippet)
                ResultValue.Value(resultField.name, resultValue, resultFieldData.second.typeName, snippetClass, snippet)
            }
        } catch (e: Throwable) {
            ResultValue.Error((e as? InvocationTargetException)?.targetException ?: e, e, snippetClass)
        }
    }
}

