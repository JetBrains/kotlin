/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.CollectAdditionalSourcesExtension
import org.jetbrains.kotlin.extensions.CompilerConfigurationExtension
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.script.ScriptDefinitionProvider
import org.jetbrains.kotlin.script.ScriptDependenciesProvider
import org.jetbrains.kotlin.script.ScriptReportSink
import org.jetbrains.kotlin.scripting.legacy.CliScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.legacy.CliScriptDependenciesProvider
import org.jetbrains.kotlin.scripting.legacy.CliScriptReportSink

class ScriptingCompilerConfigurationComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        CompilerConfigurationExtension.registerExtension(project, ScriptingCompilerConfigurationExtension(project))
        CollectAdditionalSourcesExtension.registerExtension(project, ScriptingCollectAdditionalSourcesExtension(project))

        val scriptDefinitionProvider = CliScriptDefinitionProvider()
        project.registerService(ScriptDefinitionProvider::class.java, scriptDefinitionProvider)
        project.registerService(
            ScriptDependenciesProvider::class.java,
            CliScriptDependenciesProvider(project)
        )
        SyntheticResolveExtension.registerExtension(project, ScriptingResolveExtension())

        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        if (messageCollector != null) {
            project.registerService(
                ScriptReportSink::class.java,
                CliScriptReportSink(messageCollector)
            )
        }
    }
}