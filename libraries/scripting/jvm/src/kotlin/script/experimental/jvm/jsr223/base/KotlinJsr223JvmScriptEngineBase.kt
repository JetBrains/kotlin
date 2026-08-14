/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.jsr223.base

import java.io.Reader
import java.io.Serializable
import java.lang.ref.WeakReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.script.*
import javax.script.CompiledScript
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.KJvmEvaluatedSnippet
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.jsr223.getScriptContext
import kotlin.script.experimental.jvm.jsr223.jsr223
import kotlin.script.experimental.jvm.util.isIncomplete
import kotlin.script.experimental.util.LinkedSnippet

/**
 * Keep args and arg types together, so as a whole they are present or absent
 */
class ScriptArgsWithTypes(val scriptArgs: Array<out Any?>, val scriptArgsTypes: Array<out KClass<out Any>>) : Serializable {
    init { assert(scriptArgs.size == scriptArgsTypes.size) }
    companion object {
        private val serialVersionUID: Long = 8529357500L
    }
}

const val KOTLIN_SCRIPT_STATE_BINDINGS_KEY = "kotlin.script.state"
const val KOTLIN_SCRIPT_ENGINE_BINDINGS_KEY = "kotlin.script.engine"

interface InvokeWrapper {
    operator fun <T> invoke(body: () -> T): T // e.g. for capturing io
}

/**
 * The JSR-223 scaffolding shared by every Kotlin JSR-223 engine. It covers the `Bindings`-based
 * session state (see [getCurrentState]), the live [ScriptContext] the bindings-exposure machinery
 * relies on (see [jsr223HostConfiguration]), and the compile/eval loop over a [ReplCompiler] and
 * [ReplEvaluator] pair.
 *
 * The compiler and evaluator are declared as the generic [ReplCompiler]/[ReplEvaluator] interfaces
 * rather than concrete implementations, so an alternative compiler (for example one driving snippet
 * compilation through an out-of-process compile daemon) can reuse everything here.
 *
 * A subclass supplies [replCompiler], [replEvaluator], and the [State] holding them (see
 * [createState]). It also supplies [compilationConfiguration] and [evaluationConfiguration], which
 * should be built with `hostConfiguration.update { it.withDefaultsFrom(jsr223HostConfiguration) }`
 * so the bindings-exposure refinement hooks can reach this engine's live [ScriptContext], and
 * [nextSnippetNo].
 */
abstract class KotlinJsr223JvmScriptEngineBase<State>(
    protected val myFactory: ScriptEngineFactory
) : AbstractScriptEngine(), ScriptEngine, Compilable {

    protected abstract val replCompiler: ReplCompiler<CompiledSnippet>
    protected abstract val replEvaluator: ReplEvaluator<CompiledSnippet, KJvmEvaluatedSnippet>

    /**
     * The compilation configuration every snippet is compiled with. It is a `var` because each
     * compile's resulting configuration is threaded forward into the next (see [compileSnippet]).
     * A synthetic-snippet-generating refinement hook (for example `generateBindingSnippetIfNeeded`)
     * records which bindings are already exposed as typed properties, and with which types, in it.
     */
    protected abstract var compilationConfiguration: ScriptCompilationConfiguration

    protected abstract val evaluationConfiguration: ScriptEvaluationConfiguration

    /**
     * The number the next compiled snippet's synthetic source name is built from (see
     * [compileSnippet]). Must come from the *default* context's [State] so snippet names stay unique
     * across the shared [replCompiler]. Taking it from a custom context's freshly created state would
     * restart the numbering and collide with names an outer session already compiled.
     */
    protected abstract fun nextSnippetNo(): Int

    // The custom Bindings-scoped ScriptContext of the call currently in flight, if any. It must be
    // visible to that call's synthetic-snippet generation (for example generateBindingSnippetIfNeeded),
    // not just to the default context.
    @Volatile
    private var lastScriptContext: ScriptContext? = null

    /**
     * Exposes this engine's live [ScriptContext] to the compilation and evaluation configurations'
     * refinement hooks. This is what makes JSR-223 bindings visible to a snippet as properties.
     */
    val jsr223HostConfiguration: ScriptingHostConfiguration =
        ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {
            val weakThis = WeakReference(this@KotlinJsr223JvmScriptEngineBase)
            jsr223 {
                getScriptContext { weakThis.get()?.let { it.lastScriptContext ?: it.getContext() } }
            }
        }

    override fun eval(script: String, context: ScriptContext): Any? = compileAndEval(script, context)

    override fun eval(script: Reader, context: ScriptContext): Any? = compileAndEval(script.readText(), context)

    override fun compile(script: String): CompiledScript = compile(script, getContext())

    override fun compile(script: Reader): CompiledScript = compile(script.readText(), getContext())

    override fun createBindings(): Bindings = SimpleBindings().apply { put(KOTLIN_SCRIPT_ENGINE_BINDINGS_KEY, this) }

    override fun getFactory(): ScriptEngineFactory = myFactory

    protected abstract fun createState(lock: ReentrantReadWriteLock = ReentrantReadWriteLock()): State

    @Suppress("UNCHECKED_CAST")
    protected fun getCurrentState(context: ScriptContext) =
        context.getBindings(ScriptContext.ENGINE_SCOPE)
            .getOrPut(
                KOTLIN_SCRIPT_STATE_BINDINGS_KEY,
                {
                    // TODO: createBindings is not called when the default context is created, so the engine key is missing there.
                    context.getBindings(ScriptContext.ENGINE_SCOPE).put(KOTLIN_SCRIPT_ENGINE_BINDINGS_KEY, this@KotlinJsr223JvmScriptEngineBase)
                    createState()
                }
            ) as State

    open fun overrideScriptArgs(context: ScriptContext): ScriptArgsWithTypes? = null

    open fun compile(script: String, context: ScriptContext): CompiledScript {
        lastScriptContext = context
        @Suppress("DEPRECATION_ERROR")
        val result = internalScriptingRunSuspend {
            compileSnippet(script, nextSnippetNo())
        }
        return when (result) {
            is ResultWithDiagnostics.Success -> CompiledKotlinSnippet(this, result.value)
            is ResultWithDiagnostics.Failure -> throw compileFailureException(result)
        }
    }

    open fun compileAndEval(script: String, context: ScriptContext): Any? {
        lastScriptContext = context
        return asJsr223EvalResult {
            @Suppress("DEPRECATION_ERROR")
            internalScriptingRunSuspend {
                compileSnippet(script, nextSnippetNo()).onSuccess {
                    replEvaluator.eval(it, evaluationConfiguration)
                }
            }
        }
    }

    /**
     * Hook for a compiler-specific per-snippet configuration tweak (for example setting
     * `repl.currentLineId`). Called for every snippet, with the already threaded-forward
     * [compilationConfiguration] as the base.
     */
    protected open fun snippetCompilationConfiguration(snippet: SourceCode, snippetNo: Int): ScriptCompilationConfiguration =
        compilationConfiguration

    private suspend fun compileSnippet(script: String, snippetNo: Int): ResultWithDiagnostics<LinkedSnippet<CompiledSnippet>> {
        // Suffixed onto the host template's own fileExtension (for example `main.kts`) rather than
        // hard-coded to `.kts`, so the synthetic snippet name still matches the host's own script
        // definition.
        val fileExtension = compilationConfiguration[ScriptCompilationConfiguration.fileExtension]
        val snippet = script.toScriptSource("snippet_$snippetNo.repl.$fileExtension")
        return replCompiler.compile(snippet, snippetCompilationConfiguration(snippet, snippetNo)).also {
            if (it is ResultWithDiagnostics.Success) {
                compilationConfiguration = it.value.get().compilationConfiguration
            }
        }
    }

    private fun evalCompiledSnippet(
        compiledSnippet: LinkedSnippet<CompiledSnippet>,
        context: ScriptContext,
    ): Any? {
        lastScriptContext = context
        return asJsr223EvalResult {
            @Suppress("DEPRECATION_ERROR")
            internalScriptingRunSuspend {
                replEvaluator.eval(compiledSnippet, evaluationConfiguration)
            }
        }
    }

    private fun compileFailureException(result: ResultWithDiagnostics.Failure): ScriptException =
        if (result.isIncomplete()) ScriptException("Error: incomplete code. ${result.reports.joinToString("\n")}")
        else ScriptException("Error compiling snippet:\n${result.reports.joinToString("\n")}")

    private fun asJsr223EvalResult(body: () -> ResultWithDiagnostics<LinkedSnippet<KJvmEvaluatedSnippet>>): Any? {
        val result = try {
            body()
        } catch (e: Exception) {
            throw ScriptException(e)
        }

        return when (result) {
            is ResultWithDiagnostics.Success -> when (val evaluationResult = result.value.get().result) {
                is ResultValue.Value -> evaluationResult.value
                is ResultValue.Unit -> null
                is ResultValue.Error -> throw ScriptException(
                    (evaluationResult.error as? Exception) ?: RuntimeException(evaluationResult.error)
                )
                is ResultValue.NotEvaluated -> null
            }
            is ResultWithDiagnostics.Failure -> throw compileFailureException(result)
        }
    }

    /**
     * A [CompiledScript] produced by [compile]. The snippet is compiled and recorded into
     * [replCompiler]'s history, but not yet run. [eval] runs it via the same [replEvaluator] path
     * that [compileAndEval] uses.
     */
    class CompiledKotlinSnippet internal constructor(
        val engine: KotlinJsr223JvmScriptEngineBase<*>,
        val compiledSnippet: LinkedSnippet<CompiledSnippet>,
    ) : CompiledScript() {
        override fun eval(context: ScriptContext): Any? = engine.evalCompiledSnippet(compiledSnippet, context)

        override fun getEngine(): ScriptEngine = engine
    }
}
