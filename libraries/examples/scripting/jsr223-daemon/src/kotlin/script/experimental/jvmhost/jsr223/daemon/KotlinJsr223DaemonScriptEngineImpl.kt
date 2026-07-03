/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223.daemon

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.daemon.common.DaemonJVMOptions
import org.jetbrains.kotlin.daemon.common.DaemonLogOptions
import org.jetbrains.kotlin.daemon.common.DaemonOptions
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplEvaluator
import java.io.File
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
import kotlin.script.experimental.api.onSuccess
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.KJvmEvaluatedSnippet
import kotlin.script.experimental.jvm.util.isIncomplete
import kotlin.script.experimental.jvmhost.jsr223.base.KotlinJsr223JvmScriptEngineBase
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
 *  * **No JSR-223 `Bindings`/implicit-receiver support yet.** `$$eval` is invoked with no
 *    arguments (this engine does not implement
 *    [kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223InvocableScriptEngine]).
 *  * Not registered as a `javax.script.ScriptEngineFactory` service -- callers (including this
 *    module's own tests) construct [KotlinJsr223DaemonScriptEngineFactory] directly.
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
) : KotlinJsr223JvmScriptEngineBase<DaemonReplState>(factory) {

    private val compilationConfiguration = ScriptCompilationConfiguration()
    private val evaluationConfiguration = ScriptEvaluationConfiguration()

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
    @TestOnly
    fun forceShutdownDaemonForTests() {
        replCompiler.forceShutdownDaemon()
    }

    private suspend fun compile(script: String, snippetNo: Int): ResultWithDiagnostics<LinkedSnippet<CompiledSnippet>> {
        val snippet = script.toScriptSource("snippet_$snippetNo.repl.kts")
        return replCompiler.compile(snippet, compilationConfiguration)
    }

    override fun compile(script: String, context: ScriptContext): CompiledScript {
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
            @Suppress("DEPRECATION_ERROR")
            val result = internalScriptingRunSuspend {
                engine.replEvaluator.eval(compiledSnippet, engine.evaluationConfiguration)
            }
            return engine.asJsr223EvalResult(result)
        }

        override fun getEngine(): ScriptEngine = engine
    }
}
