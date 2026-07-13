/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223

import com.google.common.base.Throwables
import org.jetbrains.kotlin.cli.common.repl.LineId
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplCompiler
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplEvaluator
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.currentLineId
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.withMessageCollectorAndDisposable
import java.lang.ref.WeakReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.script.CompiledScript
import javax.script.ScriptContext
import javax.script.ScriptEngine
import javax.script.ScriptEngineFactory
import javax.script.ScriptException
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.host.withDefaultsFrom
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.KJvmEvaluatedSnippet
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.util.isIncomplete
import kotlin.script.experimental.jvmhost.jsr223.base.InvokeWrapper
import kotlin.script.experimental.jvmhost.jsr223.base.KOTLIN_SCRIPT_STATE_BINDINGS_KEY
import kotlin.script.experimental.jvmhost.jsr223.base.KotlinJsr223JvmScriptEngineBase
import kotlin.script.experimental.jvmhost.jsr223.base.ScriptArgsWithTypes
import kotlin.script.experimental.util.LinkedSnippet

data class K2ReplState(
    val compiler: K2ReplCompiler,
    val evaluator: K2ReplEvaluator,
    var lineCounter: Int = 0,
)

class KotlinJsr223ScriptEngineImpl(
    factory: ScriptEngineFactory,
    baseCompilationConfiguration: ScriptCompilationConfiguration,
    baseEvaluationConfiguration: ScriptEvaluationConfiguration,
    val getScriptArgs: (context: ScriptContext) -> ScriptArgsWithTypes?
) : KotlinJsr223JvmScriptEngineBase<K2ReplState>(factory), KotlinJsr223InvocableScriptEngine {

    @Volatile
    internal var lastScriptContext: ScriptContext? = null

    val jsr223HostConfiguration = ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {
        val weakThis = WeakReference(this@KotlinJsr223ScriptEngineImpl)
        jsr223 {
            getScriptContext { weakThis.get()?.let { it.lastScriptContext ?: it.getContext() } }
        }
    }

    private var compilationConfiguration =
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

    private val evaluationConfiguration by lazy {
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

    private suspend fun compile(line: String, lineNo: Int): ResultWithDiagnostics<LinkedSnippet<CompiledSnippet>> {
        val lineId = LineId(lineNo, 0, line.hashCode())
        // The snippet's file extension is derived from the host template's own `fileExtension`, so that the
        // synthetic per-snippet source name still ends with an extension the host's own script
        // definition matches.
        val fileExtension = compilationConfiguration[ScriptCompilationConfiguration.fileExtension]
        val snippet = line.toScriptSource("snippet_$lineNo.repl.$fileExtension")

        return replCompiler.compile(
            snippet,
            compilationConfiguration.with {
                repl {
                    currentLineId(lineId)
                }
            }
        ).also {
            if (it is ResultWithDiagnostics.Success) {
                compilationConfiguration = it.value.get().compilationConfiguration
            }
        }
    }

    override fun compileAndEval(script: String, context: ScriptContext): Any? {
        lastScriptContext = context
        return asJsr223EvalResult {
            @Suppress("DEPRECATION_ERROR")
            internalScriptingRunSuspend {
                // Use the default context's lineCounter so snippet names are unique across all compilations by the shared replCompiler.
                // Using a custom context's lineCounter would yield lineNo=0 from a freshly-created state, colliding
                // with snippet names already compiled by the outer session compiler.
                compile(script, getCurrentState(getContext()).lineCounter++).onSuccess {
                    replEvaluator.eval(it, evaluationConfiguration)
                }
            }
        }
    }

    override fun compile(script: String, context: ScriptContext): CompiledScript {
        lastScriptContext = context
        @Suppress("DEPRECATION_ERROR")
        val result = internalScriptingRunSuspend {
            compile(script, getCurrentState(getContext()).lineCounter++)
        }
        when (result) {
            is ResultWithDiagnostics.Success -> {
                return CompiledKotlinScript(this, result.value)
            }
            is ResultWithDiagnostics.Failure -> {
                when {
                    result.isIncomplete() -> throw ScriptException("Error: incomplete code. ${result.reports.joinToString("\n")}")
                    else -> throw ScriptException("Error compiling snippet\n${result.reports.joinToString("\n")}")
                }
            }
        }
    }

    private fun asJsr223EvalResult(body: () -> ResultWithDiagnostics<LinkedSnippet<KJvmEvaluatedSnippet>>): Any? {
        val result = try {
            body()
        } catch (e: Exception) {
            throw ScriptException(e)
        }

        return when (result) {
            is ResultWithDiagnostics.Success -> {
                when (val evaluationResult = result.value.get().result) {
                    is ResultValue.Value -> evaluationResult.value
                    is ResultValue.Unit -> null
                    is ResultValue.Error ->
                        throw ScriptException(
                            (evaluationResult.error as? java.lang.Exception)
                                ?: RuntimeException(evaluationResult.error))
                    is ResultValue.NotEvaluated -> ReplEvalResult.Error.Runtime("Not evaluated")
                }
            }
            is ResultWithDiagnostics.Failure -> {
                when {
                    result.isIncomplete() -> throw ScriptException("Error: incomplete code. ${result.reports.joinToString("\n")}")
                    else -> throw ScriptException("Error evaluation thhe snippet:\n${result.reports.joinToString("\n")}")
                }
            }
        }
    }

    class CompiledKotlinScript(val engine: KotlinJsr223ScriptEngineImpl, val compiledSnippet: LinkedSnippet<CompiledSnippet>) : CompiledScript() {
        override fun eval(context: ScriptContext): Any? {
            engine.lastScriptContext = context
            return engine.asJsr223EvalResult {
                @Suppress("DEPRECATION_ERROR")
                internalScriptingRunSuspend {
                    engine.replEvaluator.eval(compiledSnippet, engine.evaluationConfiguration)
                }
            }
        }

        override fun getEngine(): ScriptEngine = engine
    }
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
