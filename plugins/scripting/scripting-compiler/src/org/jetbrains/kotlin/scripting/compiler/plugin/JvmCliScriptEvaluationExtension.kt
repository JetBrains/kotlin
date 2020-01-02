/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import com.intellij.core.JavaCoreProjectEnvironment
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptJvmCompilerFromEnvironment
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.ScriptEvaluator
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.jvm

class JvmCliScriptEvaluationExtension : AbstractScriptEvaluationExtension() {

    override fun ScriptEvaluationConfiguration.Builder.platformEvaluationConfiguration() {
        jvm {
            baseClassLoader(null)
        }
    }

    override fun setupScriptConfiguration(configuration: CompilerConfiguration) {
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

    override fun createScriptCompiler(environment: KotlinCoreEnvironment): ScriptCompilerProxy {
        return ScriptJvmCompilerFromEnvironment(environment)
    }

    override fun isAccepted(arguments: CommonCompilerArguments): Boolean =
        arguments is K2JVMCompilerArguments && (arguments.script || arguments.expressions != null)
}

