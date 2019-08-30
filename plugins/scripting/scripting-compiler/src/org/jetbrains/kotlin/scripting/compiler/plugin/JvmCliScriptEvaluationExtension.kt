/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import com.intellij.core.JavaCoreProjectEnvironment
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptJvmCompilerFromEnvironment
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator

class JvmCliScriptEvaluationExtension : AbstractScriptEvaluationExtension() {

    override fun getSourcePath(arguments: CommonCompilerArguments): String {
        return arguments.freeArgs.first()
    }

    override fun setupScriptConfiguration(configuration: CompilerConfiguration, sourcePath: String) {
        configuration.addKotlinSourceRoot(sourcePath)
        configuration.put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)
    }

    override fun createEnvironment(
        projectEnvironment: JavaCoreProjectEnvironment,
        configuration: CompilerConfiguration
    ): KotlinCoreEnvironment {
        return KotlinCoreEnvironment.createForProduction(projectEnvironment, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }

    override fun createScriptEvaluator(): ScriptEvaluator {
        return BasicJvmScriptEvaluator()
    }

    private var environment: KotlinCoreEnvironment? = null
    private val scriptCompiler: ScriptJvmCompilerFromEnvironment by lazy {
        ScriptJvmCompilerFromEnvironment(environment!!)
    }

    override suspend fun compilerInvoke(
        environment: KotlinCoreEnvironment,
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<CompiledScript<*>> {
        this.environment = environment
        return scriptCompiler.compile(script, scriptCompilationConfiguration)
    }

    override suspend fun preprocessEvaluation(
        scriptEvaluator: ScriptEvaluator,
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
        evaluationConfiguration: ScriptEvaluationConfiguration
    ) {
        //do nothing
    }

    override fun isAccepted(arguments: CommonCompilerArguments): Boolean =
        arguments is K2JVMCompilerArguments && arguments.script
}

