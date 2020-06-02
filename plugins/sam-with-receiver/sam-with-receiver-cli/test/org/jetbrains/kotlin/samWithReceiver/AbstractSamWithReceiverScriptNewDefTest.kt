/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.samWithReceiver

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.checkers.AbstractDiagnosticsTest
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
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
        SamWithReceiverComponentRegistrar().registerProjectComponents(environment.project as MockProject, environment.configuration)
    }

    override fun analyzeAndCheck(testDataFile: File, files: List<TestFile>) {
        val definition = createJvmScriptDefinitionFromTemplate<ScriptForSamWithReceiversNewDef>()
        val scriptCompiler = ScriptJvmCompilerFromEnvironment(environment)
        val (scripts, regular) = files.partition {
            it.ktFile?.virtualFile?.extension == definition.compilationConfiguration[ScriptCompilationConfiguration.fileExtension]
        }
        super.analyzeAndCheck(testDataFile, files)
        for (file in scripts) {
            val res = scriptCompiler.compile(KtFileScriptSource(file.ktFile!!), ScriptForSamWithReceiversNewDefCompilationConfiguration)
            val x = res
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
