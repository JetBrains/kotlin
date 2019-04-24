/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.scripting.resolve.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File
import java.io.IOException
import java.lang.IllegalArgumentException
import java.net.URLClassLoader
import java.util.jar.JarFile
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.KotlinType
import kotlin.script.experimental.host.createCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.templates.ScriptTemplateDefinition

const val SCRIPT_DEFINITION_MARKERS_PATH = "META-INF/kotlin/script/templates/"
const val SCRIPT_DEFINITION_MARKERS_EXTENSION_WITH_DOT = ".classname"

class ScriptDefinitionsFromClasspathDiscoverySource(
    private val classpath: List<File>,
    private val scriptResolverEnv: Map<String, Any?>,
    private val messageCollector: MessageCollector
) : ScriptDefinitionsSource {

    override val definitions: Sequence<KotlinScriptDefinition> = run {
        discoverScriptTemplatesInClasspath(
            classpath,
            this::class.java.classLoader,
            scriptResolverEnv,
            messageCollector
        )
    }
}

private const val MANIFEST_RESOURCE_NAME = "/META-INF/MANIFEST.MF"

fun discoverScriptTemplatesInClassLoader(
    classLoader: ClassLoader,
    scriptResolverEnv: Map<String, Any?>,
    messageCollector: MessageCollector
): Sequence<KotlinScriptDefinition> {
    val classpath = classLoader.getResources(MANIFEST_RESOURCE_NAME).asSequence().mapNotNull {
        try {
            File(it.toURI()).takeIf(File::exists)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
    val classpathWithLoader = SimpleClasspathWithClassLoader(classpath.toList(), classLoader)
    return scriptTemplatesDiscoverySequence(classpathWithLoader, scriptResolverEnv, messageCollector)
}

fun discoverScriptTemplatesInClasspath(
    classpath: List<File>,
    baseClassLoader: ClassLoader?,
    scriptResolverEnv: Map<String, Any?>,
    messageCollector: MessageCollector
): Sequence<KotlinScriptDefinition> {
    // TODO: try to find a way to reduce classpath (and classloader) to minimal one needed to load script definition and its dependencies
    val classpathWithLoader = LazyClasspathWithClassLoader(baseClassLoader) { classpath }

    return scriptTemplatesDiscoverySequence(classpathWithLoader, scriptResolverEnv, messageCollector)
}

private fun scriptTemplatesDiscoverySequence(
    classpathWithLoader: ClasspathWithClassLoader,
    scriptResolverEnv: Map<String, Any?>,
    messageCollector: MessageCollector
): Sequence<KotlinScriptDefinition> {
    return sequence {
        // for jar files the definition class is expected in the same jar as the discovery file
        // in case of directories, the class output may come separate from the resources, so some candidates should be deffered and processed later
        val defferedDirDependencies = ArrayList<File>()
        val defferedDefinitionCandidates = ArrayList<String>()
        for (dep in classpathWithLoader.classpath) {
            try {
                when {
                    dep.isFile && dep.extension == "jar" -> { // checking for extension is the compiler current behaviour, so the same logic is implemented here
                        JarFile(dep).use { jar ->
                            if (jar.getJarEntry(SCRIPT_DEFINITION_MARKERS_PATH) != null) {
                                val definitionNames = jar.entries().asSequence().mapNotNull {
                                    if (it.isDirectory || !it.name.startsWith(SCRIPT_DEFINITION_MARKERS_PATH)) null
                                    else it.name.removePrefix(SCRIPT_DEFINITION_MARKERS_PATH).removeSuffix(
                                        SCRIPT_DEFINITION_MARKERS_EXTENSION_WITH_DOT
                                    )
                                }.toList()
                                val (loadedDefinitions, notFoundClasses) =
                                    definitionNames.partitionLoadJarDefinitions(
                                        jar,
                                        classpathWithLoader,
                                        scriptResolverEnv,
                                        messageCollector
                                    )
                                if (notFoundClasses.isNotEmpty()) {
                                    messageCollector.report(
                                        CompilerMessageSeverity.STRONG_WARNING,
                                        "Configure scripting: unable to find script definitions [${notFoundClasses.joinToString(", ")}]"
                                    )
                                }
                                loadedDefinitions.forEach {
                                    yield(it)
                                }
                            }
                        }
                    }
                    dep.isDirectory -> {
                        defferedDirDependencies.add(dep) // there is no way to know that the dependency is fully "used" so we add it to the list anyway
                        val discoveryMarkers = File(dep, SCRIPT_DEFINITION_MARKERS_PATH).listFiles()
                        if (discoveryMarkers?.isEmpty() == false) {
                            val (foundDefinitionClasses, notFoundDefinitions) = discoveryMarkers.map { it.name }
                                .partitionLoadDirDefinitions(dep, classpathWithLoader, scriptResolverEnv, messageCollector)
                            foundDefinitionClasses.forEach {
                                yield(it)
                            }
                            defferedDefinitionCandidates.addAll(notFoundDefinitions)
                        }
                    }
                    else -> {
                        // assuming that invalid classpath entries will be reported elsewhere anyway, so do not spam user with additional warnings here
                        messageCollector.report(CompilerMessageSeverity.LOGGING, "Configure scripting: Unknown classpath entry $dep")
                    }
                }
            } catch (e: IOException) {
                messageCollector.report(
                    CompilerMessageSeverity.STRONG_WARNING, "Configure scripting: unable to process classpath entry $dep: $e"
                )
            }
        }
        var remainingDefinitionCandidates: List<String> = defferedDefinitionCandidates
        for (dep in defferedDirDependencies) {
            if (remainingDefinitionCandidates.isEmpty()) break
            try {
                val (foundDefinitionClasses, notFoundDefinitions) =
                    remainingDefinitionCandidates.partitionLoadDirDefinitions(dep, classpathWithLoader, scriptResolverEnv, messageCollector)
                foundDefinitionClasses.forEach {
                    yield(it)
                }
                remainingDefinitionCandidates = notFoundDefinitions
            } catch (e: IOException) {
                messageCollector.report(
                    CompilerMessageSeverity.STRONG_WARNING, "Configure scripting: unable to process classpath entry $dep: $e"
                )
            }
        }
        if (remainingDefinitionCandidates.isNotEmpty()) {
            messageCollector.report(
                CompilerMessageSeverity.STRONG_WARNING,
                "The following script definitions are not found in the classpath: [${remainingDefinitionCandidates.joinToString()}]"
            )
        }
    }
}

fun loadScriptTemplatesFromClasspath(
    scriptTemplates: List<String>,
    classpath: List<File>,
    dependenciesClasspath: List<File>,
    baseClassLoader: ClassLoader,
    scriptResolverEnv: Map<String, Any?>,
    messageCollector: MessageCollector
): Sequence<KotlinScriptDefinition> =
    if (scriptTemplates.isEmpty()) emptySequence()
    else sequence {
        // trying the direct classloading from baseClassloader first, since this is the most performant variant
        val (initialLoadedDefinitions, initialNotFoundTemplates) = scriptTemplates.partitionMapNotNull {
            loadScriptDefinition(
                baseClassLoader,
                it,
                scriptResolverEnv,
                messageCollector
            )
        }
        initialLoadedDefinitions.forEach {
            yield(it)
        }
        // then searching the remaining templates in the supplied classpath

        var remainingTemplates = initialNotFoundTemplates
        val classpathWithLoader =
            LazyClasspathWithClassLoader(baseClassLoader) { classpath + dependenciesClasspath }
        for (dep in classpath) {
            if (remainingTemplates.isEmpty()) break

            try {
                val (loadedDefinitions, notFoundTemplates) = when {
                    dep.isFile && dep.extension == "jar" -> { // checking for extension is the compiler current behaviour, so the same logic is implemented here
                        JarFile(dep).use { jar ->
                            remainingTemplates.partitionLoadJarDefinitions(jar, classpathWithLoader, scriptResolverEnv, messageCollector)
                        }
                    }
                    dep.isDirectory -> {
                        remainingTemplates.partitionLoadDirDefinitions(dep, classpathWithLoader, scriptResolverEnv, messageCollector)
                    }
                    else -> {
                        // assuming that invalid classpath entries will be reported elsewhere anyway, so do not spam user with additional warnings here
                        messageCollector.report(CompilerMessageSeverity.LOGGING, "Configure scripting: Unknown classpath entry $dep")
                        DefinitionsLoadPartitionResult(
                            listOf(),
                            remainingTemplates
                        )
                    }
                }
                if (loadedDefinitions.isNotEmpty()) {
                    loadedDefinitions.forEach {
                        yield(it)
                    }
                    remainingTemplates = notFoundTemplates
                }
            } catch (e: IOException) {
                messageCollector.report(
                    CompilerMessageSeverity.STRONG_WARNING,
                    "Configure scripting: unable to process classpath entry $dep: $e"
                )
            }
        }

        if (remainingTemplates.isNotEmpty()) {
            messageCollector.report(
                CompilerMessageSeverity.STRONG_WARNING,
                "Configure scripting: unable to find script definition classes: ${remainingTemplates.joinToString(", ")}"
            )
        }
    }

private data class DefinitionsLoadPartitionResult(
    val loaded: List<KotlinScriptDefinition>,
    val notFound: List<String>
)

private inline fun List<String>.partitionLoadDefinitions(
    classpathWithLoader: ClasspathWithClassLoader,
    scriptResolverEnv: Map<String, Any?>,
    messageCollector: MessageCollector,
    getBytes: (String) -> ByteArray?
): DefinitionsLoadPartitionResult {
    val loaded = ArrayList<KotlinScriptDefinition>()
    val notFound = ArrayList<String>()
    for (definitionName in this) {
        val classBytes = getBytes(definitionName)
        val definition = classBytes?.let {
            loadScriptDefinition(
                it,
                definitionName,
                classpathWithLoader,
                scriptResolverEnv,
                messageCollector
            )
        }
        when {
            definition != null -> loaded.add(definition)
            classBytes != null -> {
            }
            else -> notFound.add(definitionName)
        }
    }
    return DefinitionsLoadPartitionResult(loaded, notFound)
}

private fun List<String>.partitionLoadJarDefinitions(
    jar: JarFile,
    classpathWithLoader: ClasspathWithClassLoader,
    scriptResolverEnv: Map<String, Any?>,
    messageCollector: MessageCollector
): DefinitionsLoadPartitionResult = partitionLoadDefinitions(classpathWithLoader, scriptResolverEnv, messageCollector) { definitionName ->
    jar.getJarEntry("${definitionName.replace('.', '/')}.class")?.let { jar.getInputStream(it).readBytes() }
}

private fun List<String>.partitionLoadDirDefinitions(
    dir: File,
    classpathWithLoader: ClasspathWithClassLoader,
    scriptResolverEnv: Map<String, Any?>,
    messageCollector: MessageCollector
): DefinitionsLoadPartitionResult = partitionLoadDefinitions(classpathWithLoader, scriptResolverEnv, messageCollector) { definitionName ->
    File(dir, "${definitionName.replace('.', '/')}.class").takeIf { it.exists() && it.isFile }?.readBytes()
}

private fun loadScriptDefinition(
    templateClassBytes: ByteArray,
    templateClassName: String,
    classpathWithLoader: ClasspathWithClassLoader,
    scriptResolverEnv: Map<String, Any?>,
    messageCollector: MessageCollector
): KotlinScriptDefinition? {
    val anns = loadAnnotationsFromClass(templateClassBytes)
    for (ann in anns) {
        var def: KotlinScriptDefinition? = null
        if (ann.name == KotlinScript::class.simpleName) {
            def = LazyScriptDefinitionFromDiscoveredClass(
                anns,
                templateClassName,
                classpathWithLoader.classpath,
                messageCollector
            )
        } else if (ann.name == ScriptTemplateDefinition::class.simpleName) {
            val templateClass = classpathWithLoader.classLoader.loadClass(templateClassName).kotlin
            def = KotlinScriptDefinitionFromAnnotatedTemplate(templateClass, scriptResolverEnv, classpathWithLoader.classpath)
        }
        if (def != null) {
            messageCollector.report(
                CompilerMessageSeverity.LOGGING,
                "Configure scripting: Added template $templateClassName from ${classpathWithLoader.classpath}"
            )
            return def
        }
    }
    messageCollector.report(
        CompilerMessageSeverity.STRONG_WARNING,
        "Configure scripting: $templateClassName is not marked with any known kotlin script annotation"
    )
    return null
}

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
                val environment = defaultJvmScriptingHostConfiguration
                KotlinScriptDefinitionAdapterFromNewAPI(
                    createCompilationConfigurationFromTemplate(
                        KotlinType(cls.kotlin),
                        environment,
                        KotlinScriptDefinition::class
                    ),
                    environment
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
        // not found - not an error, return null
    } catch (ex: Exception) {
        // other exceptions - might be an error
        messageCollector.report(
            CompilerMessageSeverity.STRONG_WARNING,
            "Error on loading script definition $template: ${ex.message}"
        )
    }
    return null
}

private interface ClasspathWithClassLoader {
    val classpath: List<File>
    val classLoader: ClassLoader
}

private class SimpleClasspathWithClassLoader(
    override val classpath: List<File>,
    override val classLoader: ClassLoader
) : ClasspathWithClassLoader

private class LazyClasspathWithClassLoader(baseClassLoader: ClassLoader?, getClasspath: () -> List<File>) : ClasspathWithClassLoader {
    override val classpath by lazy { getClasspath() }
    override val classLoader by lazy { URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), baseClassLoader) }
}

private inline fun <T, R> Iterable<T>.partitionMapNotNull(fn: (T) -> R?): Pair<List<R>, List<T>> {
    val mapped = ArrayList<R>()
    val failed = ArrayList<T>()
    for (v in this) {
        val r = fn(v)
        if (r != null) {
            mapped.add(r)
        } else {
            failed.add(v)
        }
    }
    return mapped to failed
}
