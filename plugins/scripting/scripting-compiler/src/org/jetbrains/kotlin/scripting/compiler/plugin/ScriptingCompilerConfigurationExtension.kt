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
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.extensions.CompilerConfigurationExtension
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.CliScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.reporter
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.*
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.script.experimental.host.ScriptingHostConfiguration

class ScriptingCompilerConfigurationExtension(
    val project: MockProject,
    val baseHostConfiguration: ScriptingHostConfiguration
) : CompilerConfigurationExtension {

    override fun updateConfiguration(configuration: CompilerConfiguration) {

        if (!configuration.getBoolean(ScriptingConfigurationKeys.DISABLE_SCRIPTING_PLUGIN_OPTION)) {

            val projectRoot = project.run { basePath ?: baseDir?.canonicalPath }?.let(::File)
            if (projectRoot != null) {
                configuration.put(
                    ScriptingConfigurationKeys.LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION,
                    "projectRoot",
                    projectRoot
                )
            }
            val hostConfiguration = ScriptingHostConfiguration(baseHostConfiguration) {
                getEnvironment {
                    configuration.getMap(ScriptingConfigurationKeys.LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION)
                }
            }

            configureScriptDefinitions(configuration, hostConfiguration, this::class.java.classLoader)

            val scriptDefinitionProvider = ScriptDefinitionProvider.getInstance(project) as? CliScriptDefinitionProvider
            if (scriptDefinitionProvider != null) {
                scriptDefinitionProvider.setScriptDefinitionsSources(configuration.getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS_SOURCES))
                scriptDefinitionProvider.setScriptDefinitions(configuration.getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS))
            }
        }
    }

    override fun updateFileRegistry() {
        val scriptDefinitionProvider = ScriptDefinitionProvider.getInstance(project) as? CliScriptDefinitionProvider
        if (scriptDefinitionProvider != null) {
            // Register new file extensions
            val fileTypeRegistry = FileTypeRegistry.getInstance() as CoreFileTypeRegistry

            KotlinCoreEnvironment.underApplicationLock {
                scriptDefinitionProvider.getKnownFilenameExtensions().filter {
                    fileTypeRegistry.getFileTypeByExtension(it) != KotlinFileType.INSTANCE
                }.forEach {
                    fileTypeRegistry.registerFileType(KotlinFileType.INSTANCE, it)
                }
            }
        }
    }
}

internal fun configureScriptDefinitions(
    configuration: CompilerConfiguration,
    hostConfiguration: ScriptingHostConfiguration,
    classLoader: ClassLoader
) {
    val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY) ?: MessageCollector.NONE

    val explicitScriptDefinitions = configuration.getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS_CLASSES)

    if (explicitScriptDefinitions.isNotEmpty()) {
        configureScriptDefinitions(
            explicitScriptDefinitions,
            configuration,
            classLoader,
            messageCollector,
            hostConfiguration
        )
    }
    // If not disabled explicitly, we should always support at least the standard script definition
    if (!configuration.getBoolean(JVMConfigurationKeys.DISABLE_STANDARD_SCRIPT_DEFINITION) &&
        configuration.getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS).none { it.isDefault }
    ) {
        configuration.add(
            ScriptingConfigurationKeys.SCRIPT_DEFINITIONS,
            ScriptDefinition.getDefault(hostConfiguration)
        )
    }

    val definitionsFromClasspath =
        if (configuration.getBoolean(ScriptingConfigurationKeys.DISABLE_SCRIPT_DEFINITIONS_FROM_CLASSPATH_OPTION)) null
        else
            ScriptDefinitionsFromClasspathDiscoverySource(
                configuration.jvmClasspathRoots,
                hostConfiguration,
                messageCollector.reporter
            )
    val autoloadedScriptDefinitions =
        if (configuration.getBoolean(ScriptingConfigurationKeys.DISABLE_SCRIPT_DEFINITIONS_AUTOLOADING_OPTION)) null
        else AutoloadedScriptDefinitions(hostConfiguration, classLoader, messageCollector.reporter)

    configuration.addAll(
        ScriptingConfigurationKeys.SCRIPT_DEFINITIONS_SOURCES,
        listOfNotNull(definitionsFromClasspath, autoloadedScriptDefinitions)
    )
}
