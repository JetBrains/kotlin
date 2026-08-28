/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223.daemon

import org.jetbrains.kotlin.daemon.common.DaemonJVMOptions
import org.jetbrains.kotlin.daemon.common.DaemonLogOptions
import org.jetbrains.kotlin.daemon.common.DaemonOptions
import java.io.File
import java.nio.file.Path
import javax.script.ScriptEngine
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.jvmhost.jsr223.base.KotlinJsr223JvmScriptEngineFactoryBase

/**
 * Produces [KotlinJsr223DaemonScriptEngineImpl] instances. Intentionally not registered as a
 * `javax.script.ScriptEngineFactory` service, so it never competes with the in-process engine for
 * the `"kotlin"`/`"kts"` `ScriptEngineManager` registration; embedders construct it directly.
 *
 * [compilerClasspath] must contain the Kotlin compiler plus the unshaded `kotlin-scripting-compiler`
 * jar, and [additionalClasspath] the Kotlin stdlib, which the daemon compile does not add itself.
 */
class KotlinJsr223DaemonScriptEngineFactory(
    private val compilerClasspath: List<File>,
    private val additionalClasspath: List<Path> = emptyList(),
    private val daemonJVMOptions: DaemonJVMOptions? = null,
    private val daemonOptions: DaemonOptions? = null,
    private val daemonLogOptions: DaemonLogOptions? = null,
    private val baseCompilationConfiguration: ScriptCompilationConfiguration = ScriptCompilationConfiguration(),
    private val baseEvaluationConfiguration: ScriptEvaluationConfiguration = ScriptEvaluationConfiguration(),
) : KotlinJsr223JvmScriptEngineFactoryBase() {

    override fun getScriptEngine(): ScriptEngine =
        KotlinJsr223DaemonScriptEngineImpl(
            this, compilerClasspath, additionalClasspath, daemonJVMOptions, daemonOptions, daemonLogOptions,
            baseCompilationConfiguration, baseEvaluationConfiguration,
        )
}
