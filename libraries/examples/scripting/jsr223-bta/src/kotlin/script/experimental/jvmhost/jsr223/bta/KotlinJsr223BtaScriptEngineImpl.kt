/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223.bta

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

data class BtaReplState(
    val compiler: BtaReplCompiler,
    val evaluator: K2ReplEvaluator,
    var snippetCounter: Int = 0,
)

/**
 * A JSR-223 [ScriptEngine] that compiles every snippet out-of-process through the Build Tools API,
 * reusing the stock [KotlinJsr223JvmScriptEngineBase] plumbing and [K2ReplEvaluator]. `Invocable` is
 * unsupported. [close] must be called once the engine is no longer needed.
 *
 * [baseCompilationConfiguration]/[baseEvaluationConfiguration] allow plugging in a script
 * definition's own configurations (e.g. `MainKtsScript`'s). Only their evaluation-time part is
 * honored in full: the definition's `refineConfiguration` handlers (`@file:DependsOn` resolution and
 * the like) are not run on this path, since compilation happens in another process.
 */
class KotlinJsr223BtaScriptEngineImpl(
    factory: ScriptEngineFactory,
    private val compilerClasspath: List<Path>,
    private val scriptingPluginClasspath: List<Path>,
    private val additionalClasspath: List<Path> = emptyList(),
    private val daemonJvmArguments: List<String>? = null,
    private val daemonRunFilesPath: Path? = null,
    private val daemonLogsPath: Path? = null,
    private val daemonShutdownDelayMillis: Long? = null,
    baseCompilationConfiguration: ScriptCompilationConfiguration = ScriptCompilationConfiguration(),
    baseEvaluationConfiguration: ScriptEvaluationConfiguration = ScriptEvaluationConfiguration(),
) : KotlinJsr223JvmScriptEngineBase<BtaReplState>(factory) {

    override var compilationConfiguration: ScriptCompilationConfiguration =
        ScriptCompilationConfiguration(baseCompilationConfiguration) {
            hostConfiguration.update { it.withDefaultsFrom(jsr223HostConfiguration) }
        }

    override val evaluationConfiguration: ScriptEvaluationConfiguration =
        ScriptEvaluationConfiguration(baseEvaluationConfiguration) {
            hostConfiguration.update { it.withDefaultsFrom(jsr223HostConfiguration) }
        }

    override val replCompiler: BtaReplCompiler get() = getCurrentState(getContext()).compiler
    override val replEvaluator: K2ReplEvaluator get() = getCurrentState(getContext()).evaluator

    override fun createState(lock: ReentrantReadWriteLock): BtaReplState =
        BtaReplState(
            BtaReplCompiler(
                compilerClasspath, scriptingPluginClasspath, additionalClasspath,
                daemonJvmArguments = daemonJvmArguments,
                daemonRunFilesPath = daemonRunFilesPath,
                daemonLogsPath = daemonLogsPath,
                daemonShutdownDelayMillis = daemonShutdownDelayMillis,
            ),
            K2ReplEvaluator(),
        )

    override fun nextSnippetNo(): Int = getCurrentState(getContext()).snippetCounter++

    fun close() {
        replCompiler.close()
    }
}
