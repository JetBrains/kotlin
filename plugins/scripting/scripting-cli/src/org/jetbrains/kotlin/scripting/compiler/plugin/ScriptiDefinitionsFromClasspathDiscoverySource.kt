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
import kotlin.coroutines.experimental.SequenceBuilder
import kotlin.coroutines.experimental.buildSequence
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.KotlinType
import kotlin.script.experimental.api.ScriptingEnvironment
import kotlin.script.experimental.api.ScriptingEnvironmentProperties
import kotlin.script.experimental.definitions.ScriptDefinitionFromAnnotatedBaseClass
import kotlin.script.experimental.jvm.JvmGetScriptingClass
import kotlin.script.templates.ScriptTemplateDefinition

internal const val SCRIPT_DEFINITION_MARKERS_PATH = "META-INF/kotlin/script/templates/"

class ScriptDefinitionsFromClasspathDiscoverySource(
    private val classpath: List<File>,
    private val defaultScriptDefinitionClasspath: List<File>,
    private val scriptResolverEnv: Map<String, Any?>,
    private val messageCollector: MessageCollector
) : ScriptDefinitionsSource {

    override val definitions: Sequence<KotlinScriptDefinition> = run {
        discoverScriptTemplatesInClasspath(
            classpath,
            defaultScriptDefinitionClasspath,
            this::class.java.classLoader,
            scriptResolverEnv,
            messageCollector
        )
    }
}

internal fun discoverScriptTemplatesInClasspath(
    classpath: List<File>,
    defaultScriptDefinitionClasspath: List<File>,
    baseClassLoader: ClassLoader,
    scriptResolverEnv: Map<String, Any?>,
    messageCollector: MessageCollector
): Sequence<KotlinScriptDefinition> = buildSequence {
    // TODO: try to find a way to reduce classpath (and classloader) to minimal one needed to load script definition and its dependencies
    val classLoader by lazy(LazyThreadSafetyMode.PUBLICATION) {
        URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), baseClassLoader)
    }

    suspend fun SequenceBuilder<KotlinScriptDefinition>.yieldAllDirDepDefinitions(
        directoryBasedDependency: File,
        foundDefinitionClasses: List<Pair<String, ByteArray>>
    ) {
        val dependencyClasspath = listOf(directoryBasedDependency) + defaultScriptDefinitionClasspath
        val dependencyClassLoader =
            URLClassLoader(dependencyClasspath.map { it.toURI().toURL() }.toTypedArray(), baseClassLoader)
        foundDefinitionClasses.forEach { (definitionName, definitionClassBytes) ->
            loadScriptDefinition(
                definitionClassBytes, definitionName, dependencyClasspath, { dependencyClassLoader }, scriptResolverEnv, messageCollector
            )?.also {
                yield(it)
            }
        }
    }

    // for jar files the definition class is expected in the same jar as the discovery file
    // in case of directories, the class output may come separate from the resources, so some candidates should be deffered and processed later
    val defferedDirDependencies = ArrayList<File>()
    val defferedDefinitionCandidates = ArrayList<String>()
    for (dep in classpath) {
        try {
            when {
                dep.isFile && dep.extension == "jar" -> { // checking for extension is the compiler current behaviour, so the same logic is implemented here
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
                                    loadScriptDefinition(
                                        jar.getInputStream(templateClass).readBytes(),
                                        templateClassName, classpath, { classLoader }, scriptResolverEnv, messageCollector
                                    )?.also {
                                        yield(it)
                                    }
                                }
                            }
                        }
                    }
                }
                dep.isDirectory -> {
                    defferedDirDependencies.add(dep) // there is no way to know that the dependency is fully "used" so we add it to the list anyway
                    val discoveryDir = File(dep, SCRIPT_DEFINITION_MARKERS_PATH)
                    if (discoveryDir.isDirectory) {
                        val foundDefinitionClasses = discoveryDir.listFiles().map { it.name }.partitionIntoExistingDefinitions(dep, defferedDefinitionCandidates)
                        if (foundDefinitionClasses.isNotEmpty()) {
                            yieldAllDirDepDefinitions(dep, foundDefinitionClasses)
                        }
                    }
                }
                else -> {
                    // assuming that invalid classpath entries will be reported elsewhere anyway, so do not spam user with additional warnings here
                    messageCollector.report(CompilerMessageSeverity.LOGGING, "Configure scripting: Unknown classpath entry $dep")
                }
            }
        } catch (e: IOException) {
            messageCollector.report(CompilerMessageSeverity.WARNING, "Configure scripting: unable to process classpath entry $dep: $e")
        }
    }
    var remainingDefinitionCandidates = defferedDefinitionCandidates
    for (dir in defferedDirDependencies) {
        if (remainingDefinitionCandidates.isEmpty()) break
        try {
            val notFoundDefinitionCandidates = ArrayList<String>()
            val foundDefinitionClasses = remainingDefinitionCandidates.partitionIntoExistingDefinitions(dir, notFoundDefinitionCandidates)
            if (foundDefinitionClasses.isNotEmpty()) {
                remainingDefinitionCandidates = notFoundDefinitionCandidates
                yieldAllDirDepDefinitions(dir, foundDefinitionClasses)
            }
        } catch (e: IOException) {
            messageCollector.report(CompilerMessageSeverity.WARNING, "Configure scripting: unable to process classpath entry $dir: $e")
        }
    }
    if (remainingDefinitionCandidates.isNotEmpty()) {
        messageCollector.report(
            CompilerMessageSeverity.WARNING,
            "The following script definitions are not found in the classpath: [${remainingDefinitionCandidates.joinToString()}]"
        )
    }
}

private fun List<String>.partitionIntoExistingDefinitions(
    directoryBasedDependency: File,
    notFoundDefinitionCandidates: ArrayList<String>
): List<Pair<String, ByteArray>> {
    val foundDefinitionClasses = ArrayList<Pair<String, ByteArray>>() // fqn -> file contents
    for (discoveryFileCandidate in this) {
        val file = File(directoryBasedDependency, "${discoveryFileCandidate.replace('.', '/')}.class")
        if (file.exists() && file.isFile) {
            foundDefinitionClasses.add(discoveryFileCandidate to file.readBytes())
        } else {
            notFoundDefinitionCandidates.add(discoveryFileCandidate)
        }
    }
    return foundDefinitionClasses
}

internal fun loadScriptTemplatesFromClasspath(
    scriptTemplates: List<String>,
    classpath: List<File>,
    dependenciesClasspath: List<File>,
    baseClassLoader: ClassLoader,
    scriptResolverEnv: Map<String, Any?>,
    messageCollector: MessageCollector
): Sequence<KotlinScriptDefinition> = buildSequence {
    val templatesLeftToFind = ArrayList<String>()
    // trying the direct classloading from baseClassloader first, since this is the most performant variant
    for (template in scriptTemplates) {
        val def = loadScriptDefinition(baseClassLoader, template, scriptResolverEnv, messageCollector)
        if (def == null) {
            templatesLeftToFind.add(template)
        } else {
            yield(def!!)
        }
    }
    // then searching the remaining templates in the supplied classpath
    if (templatesLeftToFind.isNotEmpty()) {
        val templateClasspath by lazy(LazyThreadSafetyMode.PUBLICATION) {
            classpath + dependenciesClasspath
        }
        val classLoader by lazy(LazyThreadSafetyMode.PUBLICATION) {
            URLClassLoader(templateClasspath.map { it.toURI().toURL() }.toTypedArray(), baseClassLoader)
        }
        for (dep in classpath) {
            try {
                when {
                    dep.isFile && dep.extension == "jar" -> { // checking for extension is the compiler current behaviour, so the same logic is implemented here
                        val jar = JarFile(dep)
                        for (templateClassName in templatesLeftToFind) {
                            val templateClassEntry = jar.getJarEntry("${templateClassName.replace('.', '/')}.class")
                            if (templateClassEntry != null) {
                                loadScriptDefinition(
                                    jar.getInputStream(templateClassEntry).readBytes(),
                                    templateClassName, templateClasspath, { classLoader }, scriptResolverEnv, messageCollector
                                )?.let {
                                    templatesLeftToFind.remove(templateClassName)
                                    yield(it)
                                }
                            }
                        }
                    }
                    dep.isDirectory -> {
                        for (templateClassName in scriptTemplates) {
                            val templateClassFile = File(dep, "${templateClassName.replace('.', '/')}.class")
                            if (templateClassFile.exists()) {
                                loadScriptDefinition(
                                    templateClassFile.readBytes(),
                                    templateClassName, templateClasspath, { classLoader }, scriptResolverEnv, messageCollector
                                )?.let {
                                    templatesLeftToFind.remove(templateClassName)
                                    yield(it)
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
    if (templatesLeftToFind.isNotEmpty()) {
        messageCollector.report(
            CompilerMessageSeverity.WARNING,
            "Configure scripting: unable to find script definition classes: $templatesLeftToFind"
        )
    }
}

private fun loadScriptDefinition(
    templateClassBytes: ByteArray,
    templateClassName: String,
    templateClasspath: List<File>,
    getClassLoader: () -> ClassLoader,
    scriptResolverEnv: Map<String, Any?>,
    messageCollector: MessageCollector
): KotlinScriptDefinition? {
    val anns = loadAnnotationsFromClass(templateClassBytes)
    for (ann in anns) {
        var def: KotlinScriptDefinition? = null
        if (ann.name == KotlinScript::class.simpleName) {
            def = LazyScriptDefinitionFromDiscoveredClass(anns, templateClassName, templateClasspath, messageCollector)
        } else if (ann.name == ScriptTemplateDefinition::class.simpleName) {
            val templateClass = getClassLoader().loadClass(templateClassName).kotlin
            def = KotlinScriptDefinitionFromAnnotatedTemplate(templateClass, scriptResolverEnv, templateClasspath)
        }
        if (def != null) {
            messageCollector.report(
                CompilerMessageSeverity.LOGGING,
                "Configure scripting: Added template $templateClassName from $templateClasspath"
            )
            return def
        }
    }
    messageCollector.report(
        CompilerMessageSeverity.WARNING,
        "Configure scripting: $templateClassName is not marked with any known kotlin script annotation"
    )
    return null
}

private fun JarFile.extractClasspath(defaultClasspath: List<File>): List<File> =
    manifest.mainAttributes.getValue("Class-Path")?.split(" ")?.map(::File) ?: defaultClasspath

private fun loadScriptDefinition(
    classLoader: ClassLoader,
    template: String,
    scriptResolverEnv: Map<String, Any?>,
    messageCollector: MessageCollector
): KotlinScriptDefinition? {
    try {
        val cls = classLoader.loadClass(template)
        val def =
            if (cls.annotations.firstIsInstanceOrNull<KotlinScript>() != null) {
                KotlinScriptDefinitionAdapterFromNewAPI(
                    ScriptDefinitionFromAnnotatedBaseClass(
                        ScriptingEnvironment(
                            ScriptingEnvironmentProperties.baseClass to KotlinType(cls.kotlin),
                            ScriptingEnvironmentProperties.getScriptingClass to JvmGetScriptingClass()
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
        // return null
    } catch (ex: Exception) {
        messageCollector.report(
            CompilerMessageSeverity.ERROR,
            "Error processing script definition template $template: ${ex.message}"
        )
    }
    return null
}