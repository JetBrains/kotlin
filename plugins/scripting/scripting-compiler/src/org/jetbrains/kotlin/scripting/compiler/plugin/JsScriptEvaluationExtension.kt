/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import com.intellij.core.JavaCoreProjectEnvironment
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.platform
import org.jetbrains.kotlin.scripting.repl.js.CompiledToJsScript
import org.jetbrains.kotlin.scripting.repl.js.JsScriptCompiler
import org.jetbrains.kotlin.scripting.repl.js.JsScriptEvaluator
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.JsDependency

fun loadScriptConfiguration(configuration: CompilerConfiguration) {
    val scriptConfiguration = ScriptCompilationConfiguration {
        baseClass("kotlin.Any")
        dependencies.append(JsDependency("compiler/ir/serialization.js/build/fullRuntime/klib"))
        platform.put("JS")
    }
    configuration.add(
        ScriptingConfigurationKeys.SCRIPT_DEFINITIONS,
        ScriptDefinition.FromConfigurations(ScriptingHostConfiguration(), scriptConfiguration, null)
    )
}

class JsScriptEvaluationExtension : AbstractScriptEvaluationExtension() {

    override fun getSourcePath(arguments: CommonCompilerArguments): String {
        return (arguments as K2JSCompilerArguments).scriptPath!!
    }

    override fun setupScriptConfiguration(configuration: CompilerConfiguration, sourcePath: String) {
        loadScriptConfiguration(configuration)
    }

    override fun createEnvironment(
        projectEnvironment: JavaCoreProjectEnvironment,
        configuration: CompilerConfiguration
    ): KotlinCoreEnvironment {
        return KotlinCoreEnvironment.createForProduction(
            projectEnvironment,
            configuration,
            EnvironmentConfigFiles.JS_CONFIG_FILES
        )
    }

    override fun createScriptEvaluator(): ScriptEvaluator {
        return JsScriptEvaluator()
    }

    private var environment: KotlinCoreEnvironment? = null
    private val scriptCompiler: JsScriptCompiler by lazy {
        JsScriptCompiler(environment!!)
    }

    override suspend fun compilerInvoke(
        environment: KotlinCoreEnvironment,
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<CompiledScript<*>> {
        this.environment = environment
        return scriptCompiler.invoke(script, scriptCompilationConfiguration)
    }

    override suspend fun preprocessEvaluation(
        scriptEvaluator: ScriptEvaluator,
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
        evaluationConfiguration: ScriptEvaluationConfiguration
    ) {
        scriptEvaluator.invoke(
            CompiledToJsScript(
                scriptCompiler.scriptDependencyBinary,
                scriptCompilationConfiguration
            ),
            evaluationConfiguration
        )
    }

    override fun isAccepted(arguments: CommonCompilerArguments): Boolean {
        return arguments is K2JSCompilerArguments
    }
}
