/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.jvmhost

import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptCompilerProxy
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptJvmCompilerIsolated
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.withDefaultsFrom
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

open class JvmScriptCompiler(
    baseHostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration,
    compilerProxy: ScriptCompilerProxy? = null
) : ScriptCompiler {

    val hostConfiguration = baseHostConfiguration.withDefaultsFrom(defaultJvmScriptingHostConfiguration)

    val compilerProxy: ScriptCompilerProxy = compilerProxy ?: ScriptJvmCompilerIsolated(hostConfiguration)

    override suspend operator fun invoke(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<CompiledScript> =
        compilerProxy.compile(
            script,
            scriptCompilationConfiguration.with {
                hostConfiguration.update { it.withDefaultsFrom(this@JvmScriptCompiler.hostConfiguration) }
            }
        )
}

