/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.extensions.CompilerConfigurationExtension
import org.jetbrains.kotlin.script.StandardScriptDefinition
import java.io.File
import java.net.URLClassLoader

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

            val explicitScriptDefinitions = configuration.getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS)

            if (explicitScriptDefinitions.isNotEmpty()) {
                configureScriptDefinitions(
                    explicitScriptDefinitions,
                    configuration,
                    messageCollector,
                    scriptResolverEnv
                )
            }
            // If not disabled explicitly, we should always support at least the standard script definition
            if (!configuration.getBoolean(JVMConfigurationKeys.DISABLE_STANDARD_SCRIPT_DEFINITION) &&
                !configuration.getList(JVMConfigurationKeys.SCRIPT_DEFINITIONS).contains(StandardScriptDefinition)
            ) {
                configuration.add(JVMConfigurationKeys.SCRIPT_DEFINITIONS, StandardScriptDefinition)
            }

            configuration.add(
                JVMConfigurationKeys.SCRIPT_DEFINITIONS_SOURCES,
                ScriptDefinitionsFromClasspathDiscoverySource(
                    configuration.jvmClasspathRoots,
                    emptyList(),
                    messageCollector
                )
            )
        }
    }
}

class ScriptingCompilerConfigurationComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        CompilerConfigurationExtension.registerExtension(project, ScriptingCompilerConfigurationExtension(project))
    }
}


fun configureScriptDefinitions(
    scriptTemplates: List<String>,
    configuration: CompilerConfiguration,
    messageCollector: MessageCollector,
    scriptResolverEnv: Map<String, Any?>
) {
    val classpath = configuration.jvmClasspathRoots
    // TODO: consider using escaping to allow kotlin escaped names in class names
    if (scriptTemplates.isNotEmpty()) {
        val classloader =
            URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), Thread.currentThread().contextClassLoader)
        var hasErrors = false
        for (template in scriptTemplates) {
            val def = loadScriptDefinition(
                classloader,
                template,
                scriptResolverEnv,
                messageCollector
            )
            if (!hasErrors && def == null) hasErrors = true
            if (def != null) {
                configuration.add(JVMConfigurationKeys.SCRIPT_DEFINITIONS, def)
            }
        }
        if (hasErrors) {
            messageCollector.report(CompilerMessageSeverity.LOGGING, "(Classpath used for templates loading: $classpath)")
            return
        }
    }
}

