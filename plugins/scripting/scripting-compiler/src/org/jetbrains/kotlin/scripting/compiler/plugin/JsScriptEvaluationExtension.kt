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
import org.jetbrains.kotlin.scripting.repl.js.JsScriptDependencyCompiler
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
    private var dependencyJsCode: String? = null
    private val scriptCompiler: JsScriptCompiler by lazy {
        val env = environment ?: error("Expected environment is initialized prior to compiler instantiation")
        JsScriptCompiler(env).apply {
            dependencyJsCode = JsScriptDependencyCompiler(env.configuration, nameTables, symbolTable).compile(dependencies)
        }
    }

    override suspend fun compilerInvoke(
        environment: KotlinCoreEnvironment,
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<CompiledScript<*>> {

        this.environment = environment

        return scriptCompiler.invoke(script, scriptCompilationConfiguration).onSuccess {
            val compiledResult = it as CompiledToJsScript
            val actualResult = dependencyJsCode?.let { d ->
                dependencyJsCode = null
                CompiledToJsScript(d + "\n" + compiledResult.jsCode, compiledResult.compilationConfiguration)
            } ?: compiledResult

            ResultWithDiagnostics.Success(actualResult)
        }
    }

    override fun ScriptEvaluationConfiguration.Builder.platformEvaluationConfiguration() {

    }

    override fun isAccepted(arguments: CommonCompilerArguments): Boolean {
        return arguments is K2JSCompilerArguments
    }
}
