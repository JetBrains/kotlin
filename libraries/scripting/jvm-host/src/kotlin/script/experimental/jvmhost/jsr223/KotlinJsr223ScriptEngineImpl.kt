/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223

import com.google.common.base.Throwables
import org.jetbrains.kotlin.cli.common.repl.LineId
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplCompiler
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplEvaluator
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.currentLineId
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.withMessageCollectorAndDisposable
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.script.ScriptContext
import javax.script.ScriptEngineFactory
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.withDefaultsFrom
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.jsr223.base.InvokeWrapper
import kotlin.script.experimental.jvm.jsr223.base.KOTLIN_SCRIPT_STATE_BINDINGS_KEY
import kotlin.script.experimental.jvm.jsr223.base.KotlinJsr223JvmScriptEngineBase
import kotlin.script.experimental.jvm.jsr223.base.ScriptArgsWithTypes
import kotlin.script.experimental.jvm.jsr223.getScriptContext
import kotlin.script.experimental.jvm.jsr223.jsr223
import kotlin.script.experimental.jvm.jvm

data class K2ReplState(
    val compiler: K2ReplCompiler,
    val evaluator: K2ReplEvaluator,
    var lineCounter: Int = 0,
)

/**
 * The in-process Kotlin JSR-223 engine: [KotlinJsr223JvmScriptEngineBase]'s compile/eval loop over
 * the in-process [K2ReplCompiler]/[K2ReplEvaluator] pair.
 */
class KotlinJsr223ScriptEngineImpl(
    factory: ScriptEngineFactory,
    baseCompilationConfiguration: ScriptCompilationConfiguration,
    baseEvaluationConfiguration: ScriptEvaluationConfiguration,
    val getScriptArgs: (context: ScriptContext) -> ScriptArgsWithTypes?
) : KotlinJsr223JvmScriptEngineBase<K2ReplState>(factory), KotlinJsr223InvocableScriptEngine {

    override var compilationConfiguration: ScriptCompilationConfiguration =
        ScriptCompilationConfiguration(baseCompilationConfiguration) {
            hostConfiguration.update { it.withDefaultsFrom(jsr223HostConfiguration) }
            repl {
                // Snippet classes should be named uniquely, to avoid classloading clashes in the "eval in eval" scenario
                // TODO: consider applying the logic for any REPL, alternatively - develop other naming scheme to avoid clashes
                makeSnippetIdentifier { configuration, snippetId ->
                    val scriptContext: ScriptContext? = configuration[ScriptCompilationConfiguration.jsr223.getScriptContext]?.invoke()
                    val engineState = scriptContext?.let {
                        it.getBindings(ScriptContext.ENGINE_SCOPE)?.get(KOTLIN_SCRIPT_STATE_BINDINGS_KEY)
                    }
                    if (engineState == null) makeDefaultSnippetIdentifier(snippetId)
                    else "ScriptingHost${System.identityHashCode(engineState).toString(16)}_${makeDefaultSnippetIdentifier(snippetId)}"
                }
            }
        }

    override val evaluationConfiguration: ScriptEvaluationConfiguration by lazy {
        ScriptEvaluationConfiguration(baseEvaluationConfiguration) {
            hostConfiguration.update { it.withDefaultsFrom(jsr223HostConfiguration) }
        }
    }

    override val replCompiler: K2ReplCompiler get() = getCurrentState(getContext()).compiler
    override val replEvaluator: K2ReplEvaluator get() = getCurrentState(getContext()).evaluator

    override fun createState(lock: ReentrantReadWriteLock): K2ReplState =
        withMessageCollectorAndDisposable(disposeOnSuccess = false) { messageCollector, disposable ->
            K2ReplState(
                K2ReplCompiler(
                    K2ReplCompiler.createCompilationState(
                        messageCollector,
                        disposable,
                        compilationConfiguration
                    )
                ),
                K2ReplEvaluator()
            ).asSuccess()
        }.valueOrThrow() // TODO: consider error reporting

    // Uses the default context's lineCounter so snippet names are unique across all compilations by the shared replCompiler.
    // Using a custom context's lineCounter would yield lineNo=0 from a freshly-created state, colliding
    // with snippet names already compiled by the outer session compiler.
    override fun nextSnippetNo(): Int = getCurrentState(getContext()).lineCounter++

    override fun snippetCompilationConfiguration(snippet: SourceCode, snippetNo: Int): ScriptCompilationConfiguration =
        compilationConfiguration.with {
            repl {
                currentLineId(LineId(snippetNo, 0, snippet.text.hashCode()))
            }
        }

    override fun overrideScriptArgs(context: ScriptContext): ScriptArgsWithTypes? = getScriptArgs(context)

    override val invokeWrapper: InvokeWrapper?
        get() = null

    override val backwardInstancesHistory: Sequence<Any>
        get() = sequence {
            var lastSnippet = getCurrentState(getContext()).evaluator.lastEvaluatedSnippet
            while (lastSnippet != null) {
                lastSnippet.get().result.scriptInstance?.let { yield(it) }
                lastSnippet = lastSnippet.previous
            }
        }

    override val baseClassLoader: ClassLoader
        get() = evaluationConfiguration[ScriptEvaluationConfiguration.jvm.baseClassLoader]!!
}

fun renderReplStackTrace(cause: Throwable, startFromMethodName: String): String {
    val newTrace = arrayListOf<StackTraceElement>()
    var skip = true
    for (element in cause.stackTrace.reversed()) {
        if ("${element.className}.${element.methodName}" == startFromMethodName) {
            skip = false
        }
        if (!skip) {
            newTrace.add(element)
        }
    }

    val resultingTrace = newTrace.reversed().dropLast(1)

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UsePropertyAccessSyntax")
    (cause as java.lang.Throwable).setStackTrace(resultingTrace.toTypedArray())

    return Throwables.getStackTraceAsString(cause).trimEnd()
}
