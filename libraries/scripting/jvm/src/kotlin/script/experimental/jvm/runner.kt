/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm

import kotlinx.coroutines.runBlocking
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.baseClass
import kotlin.script.experimental.api.onFailure
import kotlin.script.experimental.host.createEvaluationConfigurationFromTemplate
import kotlin.script.experimental.jvm.impl.createScriptFromClassLoader

@Suppress("unused") // script codegen generates a call to it
fun runCompiledScript(scriptClass: Class<*>, vararg args: String) {
    val script = createScriptFromClassLoader(scriptClass.name, scriptClass.classLoader)
    val evaluator = BasicJvmScriptEvaluator()
    val baseEvaluationConfiguration =
        createEvaluationConfigurationFromTemplate(
            script.compilationConfiguration[ScriptCompilationConfiguration.baseClass]!!,
            defaultJvmScriptingHostConfiguration,
            scriptClass.kotlin
        )
    val evaluationConfiguration = ScriptEvaluationConfiguration(baseEvaluationConfiguration) {
        jvm {
            mainArguments(args)
        }
    }
    runBlocking {
        evaluator(script, evaluationConfiguration)
    }.onFailure {
        it.reports.forEach(System.err::println)
    }
}