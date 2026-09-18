/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jsr223.bta

import java.nio.file.Path
import javax.script.ScriptEngine
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.jvmhost.jsr223.base.KotlinJsr223JvmScriptEngineFactoryBase

class KotlinJsr223BtaScriptEngineFactory(
    private val compilerClasspath: List<Path>,
    private val scriptingPluginClasspath: List<Path>,
    private val additionalClasspath: List<Path> = emptyList(),
    private val daemonJvmArguments: List<String>? = null,
    private val daemonRunFilesPath: Path? = null,
    private val daemonLogsPath: Path? = null,
    private val daemonShutdownDelayMillis: Long? = null,
    private val baseCompilationConfiguration: ScriptCompilationConfiguration = ScriptCompilationConfiguration(),
    private val baseEvaluationConfiguration: ScriptEvaluationConfiguration = ScriptEvaluationConfiguration(),
) : KotlinJsr223JvmScriptEngineFactoryBase() {

    override fun getScriptEngine(): ScriptEngine =
        KotlinJsr223BtaScriptEngineImpl(
            this, compilerClasspath, scriptingPluginClasspath, additionalClasspath,
            daemonJvmArguments, daemonRunFilesPath, daemonLogsPath, daemonShutdownDelayMillis,
            baseCompilationConfiguration, baseEvaluationConfiguration,
        )
}
