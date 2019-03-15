/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.ExitCode.COMPILATION_ERROR
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.extensions.ScriptEvaluationExtension
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.script.ScriptDefinitionProvider
import org.jetbrains.kotlin.script.StandardScriptDefinition
import java.io.File

class JvmCliScriptEvaluationExtension : ScriptEvaluationExtension {
    override fun isAccepted(arguments: CommonCompilerArguments): Boolean =
        arguments is K2JVMCompilerArguments && arguments.script

    override fun eval(arguments: CommonCompilerArguments, coreEnvironment: KotlinCoreEnvironment): ExitCode {
        val configuration = coreEnvironment.configuration
        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val scriptDefinitionProvider = ScriptDefinitionProvider.getInstance(coreEnvironment.project)
        if (scriptDefinitionProvider == null) {
            messageCollector.report(ERROR, "Unable to process the script, scripting plugin is not configured")
            return COMPILATION_ERROR
        }
        val sourcePath = arguments.freeArgs.first()
        val scriptFile = File(sourcePath)
        if (scriptFile.isDirectory || !scriptDefinitionProvider.isScript(scriptFile.name)) {
            val extensionHint =
                if (configuration.get(JVMConfigurationKeys.SCRIPT_DEFINITIONS) == listOf(StandardScriptDefinition)) " (.kts)"
                else ""
            messageCollector.report(ERROR, "Specify path to the script file$extensionHint as the first argument")
            return COMPILATION_ERROR
        }
        configuration.put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)

        val scriptArgs = arguments.freeArgs.subList(1, arguments.freeArgs.size)
        return KotlinToJVMBytecodeCompiler.compileAndExecuteScript(coreEnvironment, scriptArgs)
    }
}