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
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File
import java.io.IOException
import java.net.URLClassLoader
import java.util.jar.JarFile
import kotlin.script.experimental.api.ScriptingEnvironment
import kotlin.script.experimental.api.ScriptingEnvironmentParams
import kotlin.script.experimental.definitions.ScriptDefinitionFromAnnotatedBaseClass

class ScriptingCompilerConfigurationExtension(val project: MockProject) : CompilerConfigurationExtension {

    override fun updateConfiguration(configuration: CompilerConfiguration) {

        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY) ?: MessageCollector.NONE
        val explicitScriptDefinitions = configuration.getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS)

        val scriptDefinitions =
            if (configuration.getBoolean(ScriptingConfigurationKeys.DISABLE_SCRIPT_DEFINITIONS_FROM_CLSSPATH_OPTION))
                explicitScriptDefinitions
            else
                explicitScriptDefinitions + discoverScriptTemplatesInClasspath(configuration, messageCollector)

        if (scriptDefinitions.isNotEmpty()) {
            val projectRoot = project.run { basePath ?: baseDir?.canonicalPath }?.let(::File)
            if (projectRoot != null) {
                configuration.put(
                    ScriptingConfigurationKeys.LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION,
                    "projectRoot",
                    projectRoot
                )
            }
            configureScriptDefinitions(
                scriptDefinitions,
                configuration,
                messageCollector,
                configuration.getMap(ScriptingConfigurationKeys.LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION)
            )
        }
    }
}

class ScriptingCompilerConfigurationComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        CompilerConfigurationExtension.registerExtension(project, ScriptingCompilerConfigurationExtension(project))
    }
}

private fun discoverScriptTemplatesInClasspath(configuration: CompilerConfiguration, messageCollector: MessageCollector): Iterable<String> {
    val templates = arrayListOf<String>()
    val templatesPath = "META-INF/kotlin/script/templates/"
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
                                    templates.add(templateClassName)
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
                        templates.add(it.name)
                        messageCollector.report(
                            CompilerMessageSeverity.LOGGING,
                            "Configure scripting: Added template ${it.name} from $dep"
                        )
                    }
                }
            }
            else -> messageCollector.report(CompilerMessageSeverity.WARNING, "Configure scripting: Unknown classpath entry $dep")
        }
    }
    return templates
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
            try {
                val cls = classloader.loadClass(template)
                val def =
                    if (cls.annotations.firstIsInstanceOrNull<kotlin.script.experimental.annotations.KotlinScript>() != null) {
                        KotlinScriptDefinitionAdapterFromNewAPI(
                            ScriptDefinitionFromAnnotatedBaseClass(ScriptingEnvironment(ScriptingEnvironmentParams.baseClass to cls.kotlin))
                        )
                    } else {
                        KotlinScriptDefinitionFromAnnotatedTemplate(cls.kotlin, scriptResolverEnv)
                    }
                configuration.add(JVMConfigurationKeys.SCRIPT_DEFINITIONS, def)
                messageCollector.report(
                    CompilerMessageSeverity.INFO,
                    "Added script definition $template to configuration: name = ${def.name}, " +
                            "resolver = ${def.dependencyResolver.javaClass.name}"
                )
            } catch (ex: ClassNotFoundException) {
                messageCollector.report(CompilerMessageSeverity.ERROR, "Cannot find script definition template class $template")
                hasErrors = true
            } catch (ex: Exception) {
                messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    "Error processing script definition template $template: ${ex.message}"
                )
                hasErrors = true
                break
            }
        }
        if (hasErrors) {
            messageCollector.report(CompilerMessageSeverity.LOGGING, "(Classpath used for templates loading: $classpath)")
            return
        }
    }
}
