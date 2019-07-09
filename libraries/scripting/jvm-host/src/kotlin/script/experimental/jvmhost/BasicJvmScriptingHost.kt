/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost

import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.createCompilationConfigurationFromTemplate
import kotlin.script.experimental.host.BasicScriptingHost
import kotlin.script.experimental.host.createEvaluationConfigurationFromTemplate
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

open class BasicJvmScriptingHost(
    val hostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration,
    compiler: JvmScriptCompiler = JvmScriptCompiler(hostConfiguration),
    evaluator: ScriptEvaluator = BasicJvmScriptEvaluator()
) : BasicScriptingHost(compiler, evaluator) {

    inline fun <reified T : Any> evalWithTemplate(
        script: SourceCode,
        noinline compilation: ScriptCompilationConfiguration.Builder.() -> Unit = {},
        noinline evaluation: ScriptEvaluationConfiguration.Builder.() -> Unit = {}
    ): ResultWithDiagnostics<EvaluationResult> =
        eval(
            script,
            createJvmCompilationConfigurationFromTemplate<T>(hostConfiguration, compilation),
            createJvmEvaluationConfigurationFromTemplate<T>(hostConfiguration, evaluation)
        )
}


inline fun <reified T : Any> createJvmCompilationConfigurationFromTemplate(
    hostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration,
    noinline body: ScriptCompilationConfiguration.Builder.() -> Unit = {}
): ScriptCompilationConfiguration = createCompilationConfigurationFromTemplate(
    KotlinType(T::class),
    hostConfiguration,
    ScriptCompilationConfiguration::class,
    body
)

inline fun <reified T : Any> createJvmEvaluationConfigurationFromTemplate(
    hostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration,
    noinline body: ScriptEvaluationConfiguration.Builder.() -> Unit = {}
): ScriptEvaluationConfiguration = createEvaluationConfigurationFromTemplate(
    KotlinType(T::class),
    hostConfiguration,
    ScriptEvaluationConfiguration::class,
    body
)
