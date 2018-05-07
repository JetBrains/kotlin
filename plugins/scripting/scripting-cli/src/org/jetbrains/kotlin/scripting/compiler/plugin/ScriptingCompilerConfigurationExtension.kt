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
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.script.ScriptDefinitionsSource
import org.jetbrains.kotlin.script.StandardScriptDefinition
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File
import java.io.IOException
import java.net.URLClassLoader
import java.util.jar.JarFile
import kotlin.coroutines.experimental.buildSequence
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ScriptingEnvironment
import kotlin.script.experimental.api.ScriptingEnvironmentProperties
import kotlin.script.experimental.definitions.ScriptDefinitionFromAnnotatedBaseClass

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
                    configuration,
                    scriptResolverEnv,
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

class ScriptDefinitionsFromClasspathDiscoverySource(
    private val configuration: CompilerConfiguration,
    private val scriptResolverEnv: Map<String, Any?>,
    private val messageCollector: MessageCollector
) : ScriptDefinitionsSource {

    override val definitions: Sequence<KotlinScriptDefinition> = run {
        val classpath = configuration.jvmClasspathRoots
        // TODO: consider using escaping to allow kotlin escaped names in class names
        val classloader =
            URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), Thread.currentThread().contextClassLoader)
        discoverScriptTemplatesInClasspath(configuration, messageCollector).mapNotNull {
            loadScriptDefinition(classloader, it, scriptResolverEnv, messageCollector)
        }
    }
}

private fun discoverScriptTemplatesInClasspath(configuration: CompilerConfiguration, messageCollector: MessageCollector): Sequence<String> {
    val templatesPath = "META-INF/kotlin/script/templates/"
    return buildSequence {
        for (dep in configuration.jvmClasspathRoots) {
            when {
                dep.isFile -> {
                    // this is the compiler behaviour, so the same logic implemented here
                    if (dep.extension == "jar") {
                        try {
                            with(JarFile(dep)) {
                                for (template in entries()) {
                                    if (!template.isDirectory && template.name.startsWith(templatesPath)) {
                                        val templateClassName = template.name.removePrefix(templatesPath)
                                        yield(templateClassName)
                                        messageCollector.report(
                                            CompilerMessageSeverity.LOGGING,
                                            "Configure scripting: Added template $templateClassName from $dep"
                                        )
                                    }
                                }
                            }
                        } catch (e: IOException) {
                            messageCollector.report(
                                CompilerMessageSeverity.WARNING,
                                "Configure scripting: unable to process classpath entry $dep: $e"
                            )
                        }
                    }
                }
                dep.isDirectory -> {
                    val dir = File(dep, templatesPath)
                    if (dir.isDirectory) {
                        dir.listFiles().forEach {
                            yield(it.name)
                            messageCollector.report(
                                CompilerMessageSeverity.LOGGING,
                                "Configure scripting: Added template ${it.name} from $dep"
                            )
                        }
                    }
                }
                else -> {
                    // assuming that invalid classpath entries will be reported elsewhere anyway, so do not spam user with additional warnings here
                    messageCollector.report(CompilerMessageSeverity.LOGGING, "Configure scripting: Unknown classpath entry $dep")
                }
            }
        }
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
            val def = loadScriptDefinition(classloader, template, scriptResolverEnv, messageCollector)
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

private fun loadScriptDefinition(
    classloader: URLClassLoader,
    template: String,
    scriptResolverEnv: Map<String, Any?>,
    messageCollector: MessageCollector
): KotlinScriptDefinition? {
    try {
        val cls = classloader.loadClass(template)
        val def =
            if (cls.annotations.firstIsInstanceOrNull<KotlinScript>() != null) {
                KotlinScriptDefinitionAdapterFromNewAPI(
                    ScriptDefinitionFromAnnotatedBaseClass(ScriptingEnvironment(ScriptingEnvironmentProperties.baseClass to cls.kotlin))
                )
            } else {
                KotlinScriptDefinitionFromAnnotatedTemplate(cls.kotlin, scriptResolverEnv)
            }
        messageCollector.report(
            CompilerMessageSeverity.INFO,
            "Added script definition $template to configuration: name = ${def.name}, " +
                    "resolver = ${def.dependencyResolver.javaClass.name}"
        )
        return def
    } catch (ex: ClassNotFoundException) {
        messageCollector.report(CompilerMessageSeverity.ERROR, "Cannot find script definition template class $template")
    } catch (ex: Exception) {
        messageCollector.report(
            CompilerMessageSeverity.ERROR,
            "Error processing script definition template $template: ${ex.message}"
        )
    }
    return null
}
