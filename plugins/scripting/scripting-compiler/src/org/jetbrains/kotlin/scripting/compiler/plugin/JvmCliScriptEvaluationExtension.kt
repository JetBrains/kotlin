/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.scriptingHostConfiguration
import org.jetbrains.kotlin.config.useFir
import org.jetbrains.kotlin.config.useLightTree
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptJvmCompilerFromEnvironment
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptJvmK2CompilerFromEnvironment
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.ScriptEvaluator
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.jvm

class JvmCliScriptEvaluationExtension : AbstractScriptEvaluationExtension() {

    override fun ScriptEvaluationConfiguration.Builder.platformEvaluationConfiguration() {
        jvm {
            baseClassLoader(getPlatformClassLoader())
        }
    }

    override fun setupScriptConfiguration(configuration: CompilerConfiguration) {
    }

    @K1Deprecation
    override fun createEnvironment(
        projectEnvironment: KotlinCoreEnvironment.ProjectEnvironment,
        configuration: CompilerConfiguration
    ): KotlinCoreEnvironment {
        return KotlinCoreEnvironment.createForProduction(projectEnvironment, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }

    override fun createScriptEvaluator(): ScriptEvaluator {
        return BasicJvmScriptEvaluator()
    }

    @Deprecated("Use and implement createScriptCompiler(KotlinCoreEnvironment, ScriptCompilationConfiguration) method instead")
    override fun createScriptCompiler(environment: KotlinCoreEnvironment): ScriptCompilerProxy {
        return ScriptJvmCompilerFromEnvironment(environment)
    }

    override fun createScriptCompiler(
        environment: KotlinCoreEnvironment,
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
    ): ScriptCompilerProxy {
        val configuration = environment.configuration
        return if (configuration.useFir && configuration.useLightTree) {
            ScriptJvmK2CompilerFromEnvironment(
                environment,
                (configuration.scriptingHostConfiguration as? ScriptingHostConfiguration) ?: defaultJvmScriptingHostConfiguration
            )
        } else {
            @Suppress("DEPRECATION")
            createScriptCompiler(environment)
        }
    }

    override fun isAccepted(arguments: CommonCompilerArguments): Boolean =
        arguments is K2JVMCompilerArguments && (arguments.script || arguments.expression != null)
}

private fun getPlatformClassLoader(): ClassLoader? =
    try {
        ClassLoader::class.java.getDeclaredMethod("getPlatformClassLoader")?.invoke(null) as? ClassLoader?
    } catch (_: Exception) {
        null
    }
