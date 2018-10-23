/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost

import kotlin.script.experimental.api.KotlinType
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluator
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.createCompilationConfigurationFromTemplate
import kotlin.script.experimental.host.BasicScriptingHost
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

open class BasicJvmScriptingHost(
    hostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration,
    compiler: JvmScriptCompiler = JvmScriptCompiler(hostConfiguration),
    evaluator: ScriptEvaluator = BasicJvmScriptEvaluator()
) : BasicScriptingHost(compiler, evaluator)


inline fun <reified T : Any> createJvmCompilationConfigurationFromTemplate(
    hostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration,
    noinline body: ScriptCompilationConfiguration.Builder.() -> Unit = {}
): ScriptCompilationConfiguration = createCompilationConfigurationFromTemplate(
    KotlinType(T::class),
    hostConfiguration,
    ScriptCompilationConfiguration::class,
    body
)
