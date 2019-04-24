/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import com.intellij.core.CoreFileTypeRegistry
import com.intellij.mock.MockProject
import com.intellij.openapi.fileTypes.FileTypeRegistry
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.extensions.CompilerConfigurationExtension
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.CliScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.configuration.configureScriptDefinitions
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionsFromClasspathDiscoverySource
import org.jetbrains.kotlin.scripting.definitions.StandardScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.reporter
import java.io.File

class ScriptingCompilerConfigurationExtension(val project: MockProject) : CompilerConfigurationExtension {

    override fun updateConfiguration(configuration: CompilerConfiguration) {

        if (!configuration.getBoolean(ScriptingConfigurationKeys.DISABLE_SCRIPTING_PLUGIN_OPTION)) {

            val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY) ?: MessageCollector.NONE
            val projectRoot = project.run { basePath ?: baseDir?.canonicalPath }?.let(::File)
            if (projectRoot != null) {
                configuration.put(
                    ScriptingConfigurationKeys.LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION,
                    "projectRoot",
                    projectRoot
                )
            }
            val scriptResolverEnv = configuration.getMap(ScriptingConfigurationKeys.LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION)

            val explicitScriptDefinitions = configuration.getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS_CLASSES)

            if (explicitScriptDefinitions.isNotEmpty()) {
                configureScriptDefinitions(
                    explicitScriptDefinitions,
                    configuration,
                    this::class.java.classLoader,
                    messageCollector,
                    scriptResolverEnv
                )
            }
            // If not disabled explicitly, we should always support at least the standard script definition
            if (!configuration.getBoolean(JVMConfigurationKeys.DISABLE_STANDARD_SCRIPT_DEFINITION) &&
                !configuration.getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS).contains(StandardScriptDefinition)
            ) {
                configuration.add(
                    ScriptingConfigurationKeys.SCRIPT_DEFINITIONS,
                    StandardScriptDefinition
                )
            }

            configuration.add(
                ScriptingConfigurationKeys.SCRIPT_DEFINITIONS_SOURCES,
                ScriptDefinitionsFromClasspathDiscoverySource(
                    configuration.jvmClasspathRoots,
                    configuration.get(ScriptingConfigurationKeys.LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION) ?: emptyMap(),
                    messageCollector.reporter
                )
            )
        }

        // If not disabled explicitly, we should always support at least the standard script definition
        if (!configuration.getBoolean(JVMConfigurationKeys.DISABLE_STANDARD_SCRIPT_DEFINITION) &&
            StandardScriptDefinition !in configuration.getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS)
        ) {
            configuration.add(
                ScriptingConfigurationKeys.SCRIPT_DEFINITIONS,
                StandardScriptDefinition
            )
        }

        val scriptDefinitionProvider = ScriptDefinitionProvider.getInstance(project) as? CliScriptDefinitionProvider
        if (scriptDefinitionProvider != null) {
            scriptDefinitionProvider.setScriptDefinitionsSources(configuration.getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS_SOURCES))
            scriptDefinitionProvider.setScriptDefinitions(configuration.getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS))

            // Register new file extensions
            val fileTypeRegistry = FileTypeRegistry.getInstance() as CoreFileTypeRegistry

            scriptDefinitionProvider.getKnownFilenameExtensions().filter {
                fileTypeRegistry.getFileTypeByExtension(it) != KotlinFileType.INSTANCE
            }.forEach {
                fileTypeRegistry.registerFileType(KotlinFileType.INSTANCE, it)
            }
        }

    }
}

