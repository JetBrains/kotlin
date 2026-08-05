/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * The JSR-223 scaffolding shared by every Kotlin JSR-223 engine: the `Bindings`-based session state
 * (see [getCurrentState]), the live-[ScriptContext] tracking the bindings-exposure machinery relies
 * on (see [jsr223HostConfiguration]), and the whole compile/eval loop over a [ReplCompiler] plus a
 * [ReplEvaluator].
 *
 * Both the compiler and the evaluator are declared as the generic
 * [ReplCompiler]/[ReplEvaluator] interfaces rather than as the concrete in-process K2
 * implementations, so an alternative compiler -- e.g. one that drives snippet compilation through an
 * out-of-process compile daemon (see `kotlin.script.experimental.jvmhost.jsr223.daemon`) -- can
 * reuse everything here and substitute only the compiler.
 *
 * A subclass supplies:
 *  * [replCompiler]/[replEvaluator] and the [State] holding them (see [createState]);
 *  * the [compilationConfiguration]/[evaluationConfiguration] the loop compiles/evaluates with --
 *    both are expected to be built with `hostConfiguration.update { it.withDefaultsFrom(jsr223HostConfiguration) }`,
 *    so that the bindings-exposure refinement hooks can reach this engine's live [ScriptContext];
 *  * [nextSnippetNo], which numbers the snippet sources this loop generates.
 */
abstract class KotlinJsr223JvmScriptEngineBase<State>(
    protected val myFactory: ScriptEngineFactory
) : AbstractScriptEngine(), ScriptEngine, Compilable {

    protected abstract val replCompiler: ReplCompiler<CompiledSnippet>
    protected abstract val replEvaluator: ReplEvaluator<CompiledSnippet, KJvmEvaluatedSnippet>

    /**
     * The compilation configuration every snippet is compiled with. Declared as a `var` because the
     * configuration a snippet compiles to is threaded forward into the next compile (see
     * [compileSnippet]): a synthetic-snippet-generating refinement hook (e.g.
     * `generateBindingSnippetIfNeeded`) records its own state -- which bindings are already exposed
     * as typed properties, and with which types -- in it.
     */
    protected abstract var compilationConfiguration: ScriptCompilationConfiguration

    protected abstract val evaluationConfiguration: ScriptEvaluationConfiguration

    /**
     * The number the next compiled snippet's synthetic source name is built from (see
     * [compileSnippet]). Expected to come from the *default* context's [State], so that snippet
     * names stay unique across every compilation driven by the shared [replCompiler] -- taking it
     * from a custom context's freshly created state would restart the numbering and collide with
     * the names an outer session already compiled.
     */
    protected abstract fun nextSnippetNo(): Int

    // The ScriptContext active for the current compile/eval call: a custom Bindings-scoped
    // ScriptContext passed to a given eval() call must be visible to that same call's
    // synthetic-snippet generation (e.g. generateBindingSnippetIfNeeded), not just to the default
    // context.
    @Volatile
    private var lastScriptContext: ScriptContext? = null

    /**
     * Exposes this engine's live [ScriptContext] (the one the current compile/eval call was given,
     * or the default one) to the compilation/evaluation configurations' refinement hooks -- this is
     * what makes the JSR-223 bindings visible to a snippet as ordinary properties.
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
                    // TODO: check why createBinding is not called on creating default context, so the engine is not set
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
     * Hook for a compiler-specific per-snippet configuration tweak (e.g. the in-process engine's
     * `repl.currentLineId`). Called for every snippet, with the (already threaded forward)
     * [compilationConfiguration] as the base.
     */
    protected open fun snippetCompilationConfiguration(snippet: SourceCode, snippetNo: Int): ScriptCompilationConfiguration =
        compilationConfiguration

    private suspend fun compileSnippet(script: String, snippetNo: Int): ResultWithDiagnostics<LinkedSnippet<CompiledSnippet>> {
        // The snippet's file extension is derived from (i.e. suffixed onto) the host template's own
        // `fileExtension` (e.g. `main.kts` for `MainKtsScript`, or the default `kts`), so that the
        // synthetic per-snippet source name still ends with an extension the host's own script
        // definition matches (see `ScriptDefinition.FromConfigurationsBase.isScript`) instead of
        // always hard-coding the default `.kts`.
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
     * A [CompiledScript] produced by [compile]: the snippet has already been compiled (and recorded
     * into [replCompiler]'s history for subsequent compiles), but not yet run -- [eval] runs it, via
     * the same [replEvaluator] path [compileAndEval] uses.
     */
    class CompiledKotlinSnippet internal constructor(
        val engine: KotlinJsr223JvmScriptEngineBase<*>,
        val compiledSnippet: LinkedSnippet<CompiledSnippet>,
    ) : CompiledScript() {
        override fun eval(context: ScriptContext): Any? = engine.evalCompiledSnippet(compiledSnippet, context)

        override fun getEngine(): ScriptEngine = engine
    }
}
