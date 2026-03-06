/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.samWithReceiver

import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.TestsCompilerError
import org.jetbrains.kotlin.checkers.AbstractDiagnosticsTest
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.compiler.plugin.registerExtensionsForTest
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptJvmCompilerFromEnvironment
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import org.junit.Assert
import java.io.File
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.fileExtension
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvmhost.createJvmScriptDefinitionFromTemplate

@OptIn(ObsoleteTestInfrastructure::class)
abstract class AbstractSamWithReceiverScriptNewDefTest : AbstractDiagnosticsTest() {

    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        val def = ScriptDefinition.FromTemplate(
            defaultJvmScriptingHostConfiguration,
            ScriptForSamWithReceiversNewDef::class
        )
        environment.configuration.add(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS, def)
        // need to make a single script compilation to process definition options before registering SamWithReceiver component
        val scriptCompiler = ScriptJvmCompilerFromEnvironment(environment)
        val res = scriptCompiler.compile("42".toScriptSource("\$init"), ScriptForSamWithReceiversNewDefCompilationConfiguration)
        Assert.assertTrue(res is ResultWithDiagnostics.Success<*>)
        registerExtensionsForTest(environment.project, environment.configuration) {
            with(SamWithReceiverComponentRegistrar()) { registerExtensions(it) }
        }
    }

    override fun analyzeAndCheck(testDataFile: File, files: List<TestFile>) {
        val definition = createJvmScriptDefinitionFromTemplate<ScriptForSamWithReceiversNewDef>()
        val scriptCompiler = ScriptJvmCompilerFromEnvironment(environment)
        val scripts = files.filter {
            it.ktFile?.virtualFile?.extension == definition.compilationConfiguration[ScriptCompilationConfiguration.fileExtension]
        }
        super.analyzeAndCheck(testDataFile, files)
        for (file in scripts) {
            val res = scriptCompiler.compile(KtFileScriptSource(file.ktFile!!), ScriptForSamWithReceiversNewDefCompilationConfiguration)
            if (res is ResultWithDiagnostics.Failure && !file.name.contains("error", ignoreCase = true))
                throw TestsCompilerError(
                    RuntimeException(
                        res.reports.joinToString("\n") { it.exception?.toString() ?: it.message },
                        res.reports.find { it.exception != null }?.exception
                    )
                )
        }
    }
}

@KotlinScript(compilationConfiguration = ScriptForSamWithReceiversNewDefCompilationConfiguration::class)
abstract class ScriptForSamWithReceiversNewDef

object ScriptForSamWithReceiversNewDefCompilationConfiguration : ScriptCompilationConfiguration(
    {
        compilerOptions("-P", "plugin:org.jetbrains.kotlin.samWithReceiver:annotation=SamWithReceiver1")
    }
)
