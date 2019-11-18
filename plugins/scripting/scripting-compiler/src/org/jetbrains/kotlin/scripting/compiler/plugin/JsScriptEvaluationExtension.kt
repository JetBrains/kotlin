/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import com.intellij.core.JavaCoreProjectEnvironment
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.JsScriptCompilerWithDependenciesProxy
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.withMessageCollector
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.platform
import org.jetbrains.kotlin.scripting.repl.js.*
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.JsDependency

// TODO: the code below has to be considered as temporary hack and removed ASAP.
// Actual ScriptCompilationConfiguration should be set up from CompilerConfiguration.
fun loadScriptConfiguration(configuration: CompilerConfiguration) {
    val scriptConfiguration = ScriptCompilationConfiguration {
        baseClass("kotlin.Any")
        dependencies.append(JsDependency("libraries/stdlib/js-ir/build/fullRuntime/klib"))
        platform.put("JS")
    }
    configuration.add(
        ScriptingConfigurationKeys.SCRIPT_DEFINITIONS,
        ScriptDefinition.FromConfigurations(ScriptingHostConfiguration(), scriptConfiguration, null)
    )
}

class JsScriptEvaluationExtension : AbstractScriptEvaluationExtension() {

    override fun setupScriptConfiguration(configuration: CompilerConfiguration) {
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

    private var scriptCompilerProxy: ScriptCompilerProxy? = null

    override fun createScriptCompiler(environment: KotlinCoreEnvironment): ScriptCompilerProxy {
        return scriptCompilerProxy ?: JsScriptCompilerWithDependenciesProxy(environment).also { scriptCompilerProxy = it }
    }

    override fun ScriptEvaluationConfiguration.Builder.platformEvaluationConfiguration() {}

    override fun isAccepted(arguments: CommonCompilerArguments): Boolean {
        return arguments is K2JSCompilerArguments
    }
}
