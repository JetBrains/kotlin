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
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.script.ScriptEngine
import javax.script.ScriptEngineFactory
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.hostConfiguration
import kotlin.script.experimental.host.withDefaultsFrom
import kotlin.script.experimental.jvm.jsr223.base.KotlinJsr223JvmScriptEngineBase

/**
 * Per-JSR-223-session state for the daemon-backed engine: a [DaemonReplCompiler], a
 * [K2ReplEvaluator], and the snippet counter used to name successive sources.
 */
data class DaemonReplState(
    val compiler: DaemonReplCompiler,
    val evaluator: K2ReplEvaluator,
    var snippetCounter: Int = 0,
)

/**
 * A JSR-223 [ScriptEngine] that compiles every snippet out-of-process on the Kotlin compile
 * daemon's regular compile path. It reuses the stock REPL infrastructure:
 * [KotlinJsr223JvmScriptEngineBase] for the JSR-223 state/`Bindings` plumbing and compile/eval
 * loop, and [K2ReplEvaluator] for evaluation, with [DaemonReplCompiler] supplying compilation.
 * See [DaemonReplCompiler]'s KDoc for how the daemon's compiled output becomes a real
 * [kotlin.script.experimental.jvm.impl.KJvmCompiledScript] that the stock evaluator can consume.
 *
 * Not registered as a `javax.script.ScriptEngineFactory` service; callers construct
 * [KotlinJsr223DaemonScriptEngineFactory] directly. `Invocable`
 * (`invokeFunction`/`invokeMethod`/`getInterface`) is unsupported.
 *
 * [baseCompilationConfiguration]/[baseEvaluationConfiguration] let a caller plug in a script
 * definition's own configurations (e.g. `MainKtsScript`'s) instead of the plain, definition-less
 * defaults. Every snippet still compiles as a plain, unmarked REPL snippet on the daemon's regular
 * compile path. The definition's `refineConfiguration` hooks (annotation handlers such as
 * `@file:DependsOn` resolution, `beforeCompiling` callbacks) never run, because there is no local
 * refinement step on this path. Its *evaluation-time* configuration (`constructorArgs`,
 * `implicitReceivers`, `providedProperties`, ...) is still fully honored: [DaemonReplCompiler]
 * threads the configuration into every compiled snippet, and [K2ReplEvaluator] reads it back at
 * evaluation time.
 *
 * The bindings-exposure machinery ([kotlin.script.experimental.jvm.jsr223.configureExposedJsr223Context],
 * [kotlin.script.experimental.jvm.jsr223.generateBindingSnippetIfNeeded]) is inherited from
 * [KotlinJsr223JvmScriptEngineBase] and generates its bindings-exposing synthetic snippet
 * client-side. There is no local FIR session here to run `refineConfiguration`'s `beforeCompiling`
 * hooks implicitly, so [DaemonReplCompiler.compile] runs them itself and smuggles the resulting
 * implicit-receiver *type names* across the client/daemon process boundary through a dedicated CLI
 * option. Without that, a snippet referring to such a receiver unqualified (e.g.
 * `getBindings(...)`) would fail to compile on the daemon. See [DaemonReplCompiler.compile]'s KDoc
 * for the full mechanism.
 *
 * Call [close] once this engine is no longer needed. See [DaemonReplCompiler]'s KDoc for why the
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

    override var compilationConfiguration: ScriptCompilationConfiguration =
        ScriptCompilationConfiguration(baseCompilationConfiguration) {
            hostConfiguration.update { it.withDefaultsFrom(jsr223HostConfiguration) }
        }

    override val evaluationConfiguration: ScriptEvaluationConfiguration =
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

    override fun nextSnippetNo(): Int = getCurrentState(getContext()).snippetCounter++

    /**
     * Releases the underlying [DaemonReplCompiler]'s compile-daemon session. See its KDoc. Call once
     * this engine is no longer needed.
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
}
