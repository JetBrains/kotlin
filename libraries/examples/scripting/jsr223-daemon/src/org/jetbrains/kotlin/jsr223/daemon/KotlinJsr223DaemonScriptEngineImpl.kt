/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jsr223.daemon

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.daemon.common.DaemonJVMOptions
import org.jetbrains.kotlin.daemon.common.DaemonLogOptions
import org.jetbrains.kotlin.daemon.common.DaemonOptions
import java.io.File
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.script.ScriptEngine
import javax.script.ScriptEngineFactory
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.hostConfiguration
import kotlin.script.experimental.host.withDefaultsFrom
import kotlin.script.experimental.jvm.K2ReplEvaluator
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
 * daemon's ordinary compile path. It reuses the stock REPL infrastructure:
 * [KotlinJsr223JvmScriptEngineBase] for the JSR-223 state/`Bindings` plumbing and compile/eval
 * loop, and [K2ReplEvaluator] for evaluation, with [DaemonReplCompiler] supplying compilation.
 * See [DaemonReplCompiler]'s KDoc for the details.
 *
 * Not registered as a `javax.script.ScriptEngineFactory` service in this project; callers construct
 * [KotlinJsr223DaemonScriptEngineFactory] directly.
 * `Invocable` (`invokeFunction`/`invokeMethod`/`getInterface`) is unsupported.
 *
 * [close] should be called once to release the daemon after finishing the session.
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

    fun close() {
        replCompiler.close()
    }

    @TestOnly
    fun forceShutdownDaemonForTests() {
        replCompiler.forceShutdownDaemon()
    }
}
