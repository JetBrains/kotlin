/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.jvmhost

import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.*
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

open class BasicJvmScriptingHost(
    val baseHostConfiguration: ScriptingHostConfiguration? = null,
    compiler: JvmScriptCompiler = JvmScriptCompiler(baseHostConfiguration.withDefaultsFrom(defaultJvmScriptingHostConfiguration)),
    evaluator: ScriptEvaluator = BasicJvmScriptEvaluator()
) : BasicScriptingHost(compiler, evaluator) {

    inline fun <reified T : Any> evalWithTemplate(
        script: SourceCode,
        noinline compilation: ScriptCompilationConfiguration.Builder.() -> Unit = {},
        noinline evaluation: ScriptEvaluationConfiguration.Builder.() -> Unit = {}
    ): ResultWithDiagnostics<EvaluationResult> {
        val definition =
            createJvmScriptDefinitionFromTemplate<T>(baseHostConfiguration, compilation, evaluation)
        return eval(script, definition.compilationConfiguration, definition.evaluationConfiguration)
    }
}


inline fun <reified T : Any> createJvmCompilationConfigurationFromTemplate(
    baseHostConfiguration: ScriptingHostConfiguration? = null,
    noinline body: ScriptCompilationConfiguration.Builder.() -> Unit = {}
): ScriptCompilationConfiguration = createCompilationConfigurationFromTemplate(
    KotlinType(T::class),
    baseHostConfiguration.withDefaultsFrom(defaultJvmScriptingHostConfiguration),
    ScriptCompilationConfiguration::class,
    body
)

inline fun <reified T : Any> createJvmEvaluationConfigurationFromTemplate(
    baseHostConfiguration: ScriptingHostConfiguration? = null,
    noinline body: ScriptEvaluationConfiguration.Builder.() -> Unit = {}
): ScriptEvaluationConfiguration = createEvaluationConfigurationFromTemplate(
    KotlinType(T::class),
    baseHostConfiguration.withDefaultsFrom(defaultJvmScriptingHostConfiguration),
    ScriptEvaluationConfiguration::class,
    body
)

inline fun <reified T : Any> createJvmScriptDefinitionFromTemplate(
    baseHostConfiguration: ScriptingHostConfiguration? = null,
    noinline compilation: ScriptCompilationConfiguration.Builder.() -> Unit = {},
    noinline evaluation: ScriptEvaluationConfiguration.Builder.() -> Unit = {}
): ScriptDefinition = createScriptDefinitionFromTemplate(
    KotlinType(T::class),
    baseHostConfiguration.withDefaultsFrom(defaultJvmScriptingHostConfiguration),
    ScriptCompilationConfiguration::class,
    compilation,
    evaluation
)

