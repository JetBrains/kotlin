/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import com.intellij.core.CoreFileTypeRegistry
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.MessageCollectorAccess
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.extensions.CompilerConfigurationExtension
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.CliScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.isSnippetDefinition
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.reporter
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.*
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration

class ScriptingCompilerConfigurationExtension(
    val baseHostConfiguration: ScriptingHostConfiguration,
    val scriptDefinitionProvider: ScriptDefinitionProvider?
) : CompilerConfigurationExtension {
    override fun updateConfiguration(project: Project, configuration: CompilerConfiguration) {
        val scriptDefinitionProvider = configuration.getCompilerExtensions(ScriptDefinitionProvider).firstOrNull()
        scriptDefinitionProvider.updateScriptingConfiguration(project, configuration, baseHostConfiguration, this::class.java.classLoader)
    }

    override fun updateFileRegistry(project: Project) {
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

fun ScriptDefinitionProvider?.updateScriptingConfiguration(
    project: Project,
    configuration: CompilerConfiguration,
    baseHostConfiguration: ScriptingHostConfiguration,
    classLoader: ClassLoader,
) {
    if (!configuration.getBoolean(ScriptingConfigurationKeys.DISABLE_SCRIPTING_PLUGIN_OPTION)) {
        @Suppress("DEPRECATION")
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

        configureScriptDefinitions(configuration, hostConfiguration, classLoader)

        addReplSnippetDefinitionIfStateless(configuration, hostConfiguration)

        if (this is CliScriptDefinitionProvider) {
            setScriptDefinitionsSources(configuration.getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS_SOURCES))
            setScriptDefinitions(configuration.getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS))
        }
    }
}

/**
 * Builds the `.repl.<ext>` definition for the stateless REPL-snippet mode: the discovered definition, whose refinement
 * handlers are alive, overridden by the host configuration from `repl-snippet-configuration`. The latter is normally
 * built from the same definition, so it misses only the transient properties, which the discovered definition provides.
 * Must run after [configureScriptDefinitions] and before the definitions are passed to the provider.
 */
internal fun addReplSnippetDefinitionIfStateless(
    configuration: CompilerConfiguration,
    hostConfiguration: ScriptingHostConfiguration,
) {
    if (!configuration.getBoolean(ScriptingConfigurationKeys.REPL_SNIPPET_STATELESS_MODE)) return

    val transportedConfiguration = configuration.get(ScriptingConfigurationKeys.REPL_SNIPPET_CONFIGURATION_FILE)
        ?.let(ReplSnippetConfigurationCodec::readFrom)

    val base = selectReplSnippetBaseConfiguration(configuration, hostConfiguration, transportedConfiguration)
    val baseFileExtension = base[ScriptCompilationConfiguration.fileExtension]
        ?: transportedConfiguration?.get(ScriptCompilationConfiguration.fileExtension)
        ?: "kts"

    val snippetCompilationConfiguration = ScriptCompilationConfiguration(listOfNotNull(base, transportedConfiguration)) {
        fileExtension("repl.$baseFileExtension")
        repl.isSnippetDefinition(true)
    }

    // `findDefinition` takes the first matching definition, and a plain `<ext>` definition matches
    // `.repl.<ext>` too, so the snippet definition has to precede the one it was built from.
    val definitions = ArrayList<ScriptDefinition>()
    definitions.add(ScriptDefinition.FromConfigurations(hostConfiguration, snippetCompilationConfiguration, null))
    definitions.addAll(configuration.getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS))
    configuration.put(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS, definitions)
}

private fun selectReplSnippetBaseConfiguration(
    configuration: CompilerConfiguration,
    hostConfiguration: ScriptingHostConfiguration,
    transportedConfiguration: ScriptCompilationConfiguration?,
): ScriptCompilationConfiguration {
    val discovered = configuration.getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS).filterNot { it.isDefault }
    val selected =
        discovered.singleOrNull()
            ?: transportedConfiguration?.get(ScriptCompilationConfiguration.fileExtension)?.let { transportedExtension ->
                discovered.firstOrNull { it.fileExtension == transportedExtension }
            }

    if (selected == null && configuration.getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS_CLASSES).isNotEmpty()) {
        @OptIn(MessageCollectorAccess::class) // TODO(KT-84516)
        configuration.messageCollector.report(
            CompilerMessageSeverity.ERROR,
            "REPL snippet definition: none of the definitions requested via -Xscript-definition could be loaded" +
                    " (definition classpath: ${configuration.getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS_CLASSPATH)})"
        )
    }

    return selected?.compilationConfiguration
        ?: transportedConfiguration
        ?: ScriptDefinition.getDefault(hostConfiguration).compilationConfiguration
}

internal fun configureScriptDefinitions(
    configuration: CompilerConfiguration,
    hostConfiguration: ScriptingHostConfiguration,
    classLoader: ClassLoader
) {
    @OptIn(MessageCollectorAccess::class) // TODO(KT-84516)
    val messageCollector = configuration.messageCollector

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

    // Discovery deliberately searches the compilation classpath only: the definitions classpath serves the explicitly
    // requested definitions (see `configureScriptDefinitions` in configuration.kt), not the discovery of more of them.
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
