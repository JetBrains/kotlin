/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.jvmhost

import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptJvmCompilerProxy
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptJvmCompilerIsolated
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvmhost.impl.withDefaults

open class JvmScriptCompiler(
    baseHostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration,
    compilerProxy: ScriptJvmCompilerProxy? = null
) : ScriptCompiler {

    val hostConfiguration = baseHostConfiguration.withDefaults()

    val compilerProxy: ScriptJvmCompilerProxy = compilerProxy ?: ScriptJvmCompilerIsolated(hostConfiguration)

    override suspend operator fun invoke(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<CompiledScript<*>> =
        compilerProxy.compile(
            script,
            scriptCompilationConfiguration.with {
                hostConfiguration(this@JvmScriptCompiler.hostConfiguration)
            }
        )
}

