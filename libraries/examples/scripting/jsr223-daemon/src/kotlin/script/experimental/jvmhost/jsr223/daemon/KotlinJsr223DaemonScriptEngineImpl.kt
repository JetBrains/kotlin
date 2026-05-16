/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223.daemon

import org.jetbrains.kotlin.daemon.common.DaemonJVMOptions
import org.jetbrains.kotlin.daemon.common.DaemonLogOptions
import org.jetbrains.kotlin.daemon.common.DaemonOptions
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplEvaluator
import java.io.File
import java.lang.ref.WeakReference
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.script.CompiledScript
import javax.script.ScriptContext
import javax.script.ScriptEngine
import javax.script.ScriptEngineFactory
import javax.script.ScriptException
import kotlin.script.experimental.api.CompiledSnippet
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.fileExtension
import kotlin.script.experimental.api.hostConfiguration
import kotlin.script.experimental.api.onSuccess
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.host.withDefaultsFrom
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.KJvmEvaluatedSnippet
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.util.isIncomplete
import kotlin.script.experimental.jvmhost.jsr223.base.KotlinJsr223JvmScriptEngineBase
import kotlin.script.experimental.jvmhost.jsr223.getScriptContext
import kotlin.script.experimental.jvmhost.jsr223.jsr223
import kotlin.script.experimental.util.LinkedSnippet

/**
 * Per-JSR-223-session state, mirroring
 * [kotlin.script.experimental.jvmhost.jsr223.K2ReplState] -- the only difference is [compiler]'s
 * type: a [DaemonReplCompiler] rather than an in-process
 * [org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplCompiler].
 */
data class DaemonReplState(
    val compiler: DaemonReplCompiler,
    val evaluator: K2ReplEvaluator,
    var snippetCounter: Int = 0,
)

/**
 * A JSR-223 [ScriptEngine] that compiles every snippet **out-of-process**, on the Kotlin compile
 * daemon's regular compile path (see [DaemonReplCompiler]'s KDoc), instead of embedding the
 * compiler in-process the way
 * [kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223ScriptEngineImpl] does.
 *
 * Unlike an earlier iteration of this module (which drove a bespoke, reflection-based
 * `DaemonReplSnippetSession` and a from-scratch `DaemonReplSnippetCompiler`-only compile loop),
 * this class is built directly on top of the **stock** REPL infrastructure that
 * [kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223ScriptEngineImpl] itself uses:
 *  * [KotlinJsr223JvmScriptEngineBase] supplies the JSR-223 state/`Bindings` plumbing and the
 *    `compile`/`eval` entry points, exactly as it does for the in-process engine.
 *  * [K2ReplEvaluator] runs every compiled snippet completely unmodified -- including its
 *    existing cross-snippet classloader chaining (`ScriptEvaluationConfiguration.jvm.lastSnippetClassLoader`),
 *    which resolves references to earlier snippets' declarations without any extra code here.
 *
 * The **only** substitution is [DaemonReplCompiler] in place of `K2ReplCompiler` -- see its KDoc
 * for how the daemon's wire artifact is turned into a real
 * [kotlin.script.experimental.jvm.impl.KJvmCompiledScript] that the stock evaluator can consume.
 *
 * ### Scope of this first cut
 *
 *  * **No `Invocable` support.** `invokeFunction`/`invokeMethod`/`getInterface` are unimplemented
 *    (this engine does not implement
 *    [kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223InvocableScriptEngine]) -- see the
 *    "Bindings" section below for what *is* supported.
 *  * Not registered as a `javax.script.ScriptEngineFactory` service -- callers (including this
 *    module's own tests) construct [KotlinJsr223DaemonScriptEngineFactory] directly.
 *
 * ### Supplying a custom script definition
 *
 * [compilationConfiguration]/[evaluationConfiguration] let a caller plug in a script definition's
 * own configurations -- e.g.
 * `createJvmScriptDefinitionFromTemplate<org.jetbrains.kotlin.mainKts.MainKtsScript>()`'s -- instead
 * of the plain, definition-less defaults this class used to hardcode. Every snippet still
 * physically compiles as a **plain, unmarked REPL snippet** on the daemon's regular compile path
 * (see [DaemonReplCompiler]'s KDoc) -- named `snippet_N.repl.<extension>` (see [compile]), where
 * `<extension>` is [compilationConfiguration]'s own [ScriptCompilationConfiguration.fileExtension]
 * (e.g. `main.kts` for `MainKtsScript`), so [DaemonReplCompiler] recognizes it as such even for a
 * non-default definition -- but [compilationConfiguration] is *not* otherwise fed through the
 * daemon compile itself -- its `refineConfiguration` hooks (annotation handlers such as
 * `MainKtsScriptDefinition`'s `@file:Import`/`@file:DependsOn` resolver, `beforeCompiling`
 * callbacks, etc.) never run, since there is no in-process refinement step on this path at all.
 * What it *does* do: [DaemonReplCompiler] threads it straight into every compiled snippet's own
 * [kotlin.script.experimental.jvm.impl.KJvmCompiledScript.compilationConfiguration], which
 * [K2ReplEvaluator] reads back at evaluation time (`compiledSnippet.compilationConfiguration`) and
 * merges into [evaluationConfiguration] before running that snippet's own
 * `refineConfigurationBeforeEvaluate` hooks -- e.g. `MainKtsEvaluationConfiguration`'s
 * `configureConstructorArgsFromMainArgs`. In short: a script definition's *compile-time* dependency
 * resolution/import machinery is out of scope here, but its *evaluation-time* configuration
 * (`constructorArgs`, `implicitReceivers`, `providedProperties`, ...) is fully honored.
 *
 * ### Bindings support
 *
 * This class wires the same [jsr223HostConfiguration]/`getScriptContext` closure
 * [kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223ScriptEngineImpl] does, so that a
 * [baseCompilationConfiguration] which reuses
 * [kotlin.script.experimental.jvmhost.jsr223.configureExposedJsr223Context]/
 * [kotlin.script.experimental.jvmhost.jsr223.generateBindingSnippetIfNeeded] (e.g.
 * `KotlinJsr223DefaultScriptCompilationConfiguration`, `MainKtsScript`) generates its
 * bindings-exposing synthetic snippet -- and its implicit-receiver additions
 * (`ScriptContext`/`ScriptTemplateWithBindings`) -- client-side, exactly as the in-process engine
 * does. Unlike the in-process engine, though, there is no local FIR session here to run
 * `refineConfiguration`'s `beforeCompiling` hooks implicitly, so [DaemonReplCompiler.compile]
 * explicitly runs them itself (`refineBeforeCompiling`) and additionally has to smuggle the
 * resulting implicit-receiver *type names* across the client/daemon process boundary through a
 * dedicated CLI option, since a snippet's *source* referring to such a receiver unqualified (e.g.
 * `getBindings(...)`) would otherwise fail to even compile on the daemon -- see
 * [DaemonReplCompiler.compile]'s KDoc for the full mechanism and why this is the one piece of a
 * script definition's `ScriptCompilationConfiguration` that genuinely has to cross that boundary.
 * End to end, a value read from or put into `getContext()`'s (or an explicitly passed
 * `ScriptContext`'s) `Bindings` is visible to (and can be written back from) a snippet as an
 * ordinary property, the same as with the in-process engine.
 *
 * Call [close] once this engine is no longer needed -- see [DaemonReplCompiler]'s KDoc for why the
 * underlying compile-daemon connection must be disposed explicitly rather than per-compile.
 */
class KotlinJsr223DaemonScriptEngineImpl(
    factory: ScriptEngineFactory,
    private val compilerClasspath: List<File>,
    private val additionalClasspath: List<Path> = emptyList(),
    private val daemonJVMOptions: DaemonJVMOptions? = null,
    private val daemonOptions: DaemonOptions? = null,
    private val daemonLogOptions: DaemonLogOptions? = null,
    baseCompilationConfiguration: ScriptCompilationConfiguration = ScriptCompilationConfiguration(),
    baseEvaluationConfiguration: ScriptEvaluationConfiguration = ScriptEvaluationConfiguration(),
) : KotlinJsr223JvmScriptEngineBase<DaemonReplState>(factory) {

    // The ScriptContext active for the current compile/eval call -- see KotlinJsr223ScriptEngineImpl's
    // identical property for why this (rather than always calling getContext()) is needed: a custom
    // Bindings-scoped ScriptContext passed to a given eval() call must be visible to that same call's
    // synthetic-snippet generation (generateBindingSnippetIfNeeded), not just to the default context.
    @Volatile
    private var lastScriptContext: ScriptContext? = null

    private val jsr223HostConfiguration = ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {
        val weakThis = WeakReference(this@KotlinJsr223DaemonScriptEngineImpl)
        jsr223 {
            getScriptContext { weakThis.get()?.let { it.lastScriptContext ?: it.getContext() } }
        }
    }

    // Threaded forward after every successful compile (see compile(String, Int)) so that
    // rootBindingsConfigured/exposedBindings state -- written back by generateBindingSnippetIfNeeded
    // onto the main snippet's own compiled configuration -- carries over to the next eval, exactly as
    // KotlinJsr223ScriptEngineImpl.compile does.
    private var compilationConfiguration =
        ScriptCompilationConfiguration(baseCompilationConfiguration) {
            hostConfiguration.update { it.withDefaultsFrom(jsr223HostConfiguration) }
        }

    private val evaluationConfiguration =
        ScriptEvaluationConfiguration(baseEvaluationConfiguration) {
            hostConfiguration.update { it.withDefaultsFrom(jsr223HostConfiguration) }
        }

    override val replCompiler: DaemonReplCompiler get() = getCurrentState(getContext()).compiler
    override val replEvaluator: K2ReplEvaluator get() = getCurrentState(getContext()).evaluator

    override fun createState(lock: ReentrantReadWriteLock): DaemonReplState =
        DaemonReplState(
            DaemonReplCompiler(
                compilerClasspath, additionalClasspath,
                daemonJVMOptions = daemonJVMOptions, daemonOptions = daemonOptions, daemonLogOptions = daemonLogOptions,
            ),
            K2ReplEvaluator(),
        )

    /**
     * Releases the underlying [DaemonReplCompiler]'s compile-daemon session (see its KDoc). Call
     * once this engine is no longer needed.
     */
    fun close() {
        replCompiler.close()
    }

    /**
     * Test-only hook: see [DaemonReplCompiler.forceShutdownDaemon]'s KDoc.
     */
    fun forceShutdownDaemonForTests() {
        replCompiler.forceShutdownDaemon()
    }

    private suspend fun compile(script: String, snippetNo: Int): ResultWithDiagnostics<LinkedSnippet<CompiledSnippet>> {
        // The snippet's file extension is derived from (i.e. suffixed onto) the host template's own
        // `fileExtension` (e.g. `main.kts` for `MainKtsScript`, or the default `kts`) -- mirroring
        // `KotlinJsr223ScriptEngineImpl.compile` -- so that the synthetic per-snippet source name
        // still ends with an extension [DaemonReplCompiler] forwards to the daemon (see its KDoc)
        // instead of always hard-coding the default `.repl.kts`.
        val fileExtension = compilationConfiguration[ScriptCompilationConfiguration.fileExtension]
        val snippet = script.toScriptSource("snippet_$snippetNo.repl.$fileExtension")
        return replCompiler.compile(snippet, compilationConfiguration).also {
            if (it is ResultWithDiagnostics.Success) {
                compilationConfiguration = it.value.get().compilationConfiguration
            }
        }
    }

    override fun compile(script: String, context: ScriptContext): CompiledScript {
        lastScriptContext = context
        @Suppress("DEPRECATION_ERROR")
        val result = internalScriptingRunSuspend {
            compile(script, getCurrentState(getContext()).snippetCounter++)
        }
        return when (result) {
            is ResultWithDiagnostics.Success -> CompiledKotlinDaemonScript(this, result.value)
            is ResultWithDiagnostics.Failure -> throw compileFailureException(result)
        }
    }

    override fun compileAndEval(script: String, context: ScriptContext): Any? {
        lastScriptContext = context
        @Suppress("DEPRECATION_ERROR")
        val result = internalScriptingRunSuspend {
            compile(script, getCurrentState(getContext()).snippetCounter++).onSuccess {
                replEvaluator.eval(it, evaluationConfiguration)
            }
        }
        return asJsr223EvalResult(result)
    }

    private fun compileFailureException(result: ResultWithDiagnostics.Failure): ScriptException =
        if (result.isIncomplete()) ScriptException("Error: incomplete code. ${result.reports.joinToString("\n")}")
        else ScriptException("Error compiling Kotlin snippet:\n${result.reports.joinToString("\n")}")

    private fun asJsr223EvalResult(result: ResultWithDiagnostics<LinkedSnippet<KJvmEvaluatedSnippet>>): Any? =
        when (result) {
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

    /**
     * A [CompiledScript] produced by [compile]: the snippet has already been compiled (and its
     * artifact recorded into [replCompiler]'s history for subsequent compiles), but not yet run --
     * [eval] runs it, via the same [K2ReplEvaluator] path [compileAndEval] uses.
     */
    class CompiledKotlinDaemonScript internal constructor(
        private val engine: KotlinJsr223DaemonScriptEngineImpl,
        private val compiledSnippet: LinkedSnippet<CompiledSnippet>,
    ) : CompiledScript() {
        override fun eval(context: ScriptContext): Any? {
            engine.lastScriptContext = context
            @Suppress("DEPRECATION_ERROR")
            val result = internalScriptingRunSuspend {
                engine.replEvaluator.eval(compiledSnippet, engine.evaluationConfiguration)
            }
            return engine.asJsr223EvalResult(result)
        }

        override fun getEngine(): ScriptEngine = engine
    }
}
