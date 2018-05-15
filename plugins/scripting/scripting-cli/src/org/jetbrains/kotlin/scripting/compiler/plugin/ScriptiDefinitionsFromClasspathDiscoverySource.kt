/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.script.ScriptDefinitionsSource
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

internal const val SCRIPT_DEFINITION_MARKERS_PATH = "META-INF/kotlin/script/templates/"

class ScriptDefinitionsFromClasspathDiscoverySource(
    private val classpath: List<File>,
    private val defaultScriptDefinitionClasspath: List<File>,
    private val messageCollector: MessageCollector
) : ScriptDefinitionsSource {

    override val definitions: Sequence<KotlinScriptDefinition> = run {
        discoverScriptTemplatesInClasspath(
            classpath,
            defaultScriptDefinitionClasspath,
            messageCollector
        )
    }
}

internal fun discoverScriptTemplatesInClasspath(
    classpath: Iterable<File>,
    defaultScriptDefinitionClasspath: List<File>,
    messageCollector: MessageCollector
): Sequence<LazyScriptDefinitionFromDiscoveredClass> = buildSequence {
    for (dep in classpath) {
        try {
            when {
                // checking for extension is the compiler current behaviour, so the same logic is implemented here
                dep.isFile && dep.extension == "jar" -> {
                    val jar = JarFile(dep)
                    if (jar.getJarEntry(SCRIPT_DEFINITION_MARKERS_PATH) != null) {
                        for (template in jar.entries()) {
                            if (!template.isDirectory && template.name.startsWith(SCRIPT_DEFINITION_MARKERS_PATH)) {
                                val templateClassName = template.name.removePrefix(SCRIPT_DEFINITION_MARKERS_PATH)
                                val templateClass = jar.getJarEntry("${templateClassName.replace('.', '/')}.class")
                                if (templateClass == null) {
                                    messageCollector.report(
                                        CompilerMessageSeverity.WARNING,
                                        "Configure scripting: class not found $templateClassName"
                                    )
                                } else {
                                    messageCollector.report(
                                        CompilerMessageSeverity.LOGGING,
                                        "Configure scripting: Added template $templateClassName from $dep"
                                    )
                                    yield(
                                        LazyScriptDefinitionFromDiscoveredClass(
                                            jar.getInputStream(templateClass).readBytes(),
                                            templateClassName, listOf(dep) + jar.extractClasspath(defaultScriptDefinitionClasspath),
                                            messageCollector
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                dep.isDirectory -> {
                    val dir = File(dep, SCRIPT_DEFINITION_MARKERS_PATH)
                    if (dir.isDirectory) {
                        dir.listFiles().forEach {
                            val templateClass = File(dep, "${it.name.replace('.', '/')}.class")
                            if (!templateClass.exists() || !templateClass.isFile) {
                                messageCollector.report(
                                    CompilerMessageSeverity.WARNING,
                                    "Configure scripting: class not found ${it.name}"
                                )
                            } else {
                                messageCollector.report(
                                    CompilerMessageSeverity.LOGGING,
                                    "Configure scripting: Added template ${it.name} from $dep"
                                )
                                yield(
                                    LazyScriptDefinitionFromDiscoveredClass(
                                        templateClass.readBytes(),
                                        it.name, listOf(dep) + defaultScriptDefinitionClasspath,
                                        messageCollector
                                    )
                                )
                            }
                        }
                    }
                }
                else -> {
                    // assuming that invalid classpath entries will be reported elsewhere anyway, so do not spam user with additional warnings here
                    messageCollector.report(
                        CompilerMessageSeverity.LOGGING,
                        "Configure scripting: Unknown classpath entry $dep"
                    )
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

private fun JarFile.extractClasspath(defaultClasspath: List<File>): List<File> =
    manifest.mainAttributes.getValue("Class-Path")?.split(" ")?.map(::File) ?: defaultClasspath

internal fun loadScriptDefinition(
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
                    ScriptDefinitionFromAnnotatedBaseClass(
                        ScriptingEnvironment(
                            ScriptingEnvironmentProperties.baseClass to cls.kotlin
                        )
                    )
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