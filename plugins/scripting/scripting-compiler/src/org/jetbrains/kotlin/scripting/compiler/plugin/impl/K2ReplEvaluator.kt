/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.scripting.compiler.plugin.irLowerings.REPL_SNIPPET_EVAL_FUN_NAME
import java.lang.invoke.MethodHandleProxies
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.InvocationTargetException
import java.util.function.Consumer
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
                    (it.get().result.scriptClass?.java ?: it.get().result.scriptInstance?.javaClass)?.let {
                        lastSnippetClassLoader(it.classLoader)
                    }
                }
            }
        } ?: configuration

        return compiledSnippet.getClass(currentConfiguration).onSuccess { snippetClass ->
            evalSnippet(compiledSnippet, snippetClass, currentConfiguration).let { evaluationResult ->
                LinkedSnippetImpl(
                    KJvmEvaluatedSnippet(compiledSnippet, currentConfiguration, evaluationResult),
                    lastEvaluatedSnippet
                ).also {
                    lastEvaluatedSnippet = it
                }
            }.asSuccess()
        }
    }

    private fun evalSnippet(
        compiledSnippet: KJvmCompiledScript,
        snippetClass: KClass<*>,
        configuration: ScriptEvaluationConfiguration,
    ): ResultValue {
        val evalFunName = REPL_SNIPPET_EVAL_FUN_NAME.asString()
        val eval = snippetClass.java.methods.find { it.name == evalFunName }!!

        val snippet = snippetClass.java.getField("INSTANCE").get(null)

        val args = mutableListOf<Any?>()

        configuration[ScriptEvaluationConfiguration.implicitReceivers]?.let {
            args.addAll(it)
        }

        return try {
            runSuspend(snippetClass.java.classLoader) { cont ->
                eval.invoke(snippet, *args.toTypedArray<Any?>(), cont)
            }

            compiledSnippet.resultField?.let { (resultFieldName, resultType) ->
                val resultField = snippetClass.java.getDeclaredField(resultFieldName)
                when (resultType.typeName) {
                    "kotlin.Unit" -> ResultValue.Unit(snippetClass, snippet)
                    else -> ResultValue.Value(resultFieldName, resultField.get(snippet), resultType.typeName, snippetClass, snippet)
                }
            } ?: ResultValue.Unit(snippetClass, snippet)
        } catch (e: Throwable) {
            ResultValue.Error((e as? InvocationTargetException)?.targetException ?: e, e, snippetClass)
        }
    }

    private fun runSuspend(classLoader: ClassLoader, block: (continuation: Any) -> Unit) {
        val runSuspend = classLoader
            .loadClass("kotlin.coroutines.jvm.internal.RunSuspendKt")
            .methods
            .find { it.name == "runSuspend" }!!

        val argument = MethodHandleProxies.asInterfaceInstance(
            runSuspend.parameterTypes[0],
            MethodHandles.lookup().findVirtual(
                Consumer::class.java,
                "accept",
                MethodType.methodType(Void.TYPE, Any::class.java)
            ).bindTo(Consumer<Any> { cont -> block(cont) })
        )

        runSuspend.invoke(null, argument)
    }
}

