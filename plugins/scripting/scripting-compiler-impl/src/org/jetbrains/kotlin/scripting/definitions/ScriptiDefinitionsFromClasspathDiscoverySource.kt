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
import java.net.URLClassLoader
import java.util.jar.JarFile
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.KotlinType
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.createCompilationConfigurationFromTemplate
import kotlin.script.experimental.host.createEvaluationConfigurationFromTemplate
import kotlin.script.templates.ScriptTemplateDefinition

const val SCRIPT_DEFINITION_MARKERS_PATH = "META-INF/kotlin/script/templates/"
const val SCRIPT_DEFINITION_MARKERS_EXTENSION_WITH_DOT = ".classname"

typealias MessageReporter = (CompilerMessageSeverity, String) -> Unit

val MessageCollector.reporter: MessageReporter
    get() = { severity, message ->
        this.report(severity, message)
    }

class ScriptDefinitionsFromClasspathDiscoverySource(
    private val classpath: List<File>,
    private val hostConfiguration: ScriptingHostConfiguration,
    private val messageReporter: MessageReporter
) : ScriptDefinitionsSource {

    override val definitions: Sequence<ScriptDefinition> = run {
        discoverScriptTemplatesInClasspath(
            classpath,
            this::class.java.classLoader,
            hostConfiguration,
            messageReporter
        )
    }
}

private const val MANIFEST_RESOURCE_NAME = "/META-INF/MANIFEST.MF"

fun discoverScriptTemplatesInClassLoader(
    classLoader: ClassLoader,
    hostConfiguration: ScriptingHostConfiguration,
    messageReporter: MessageReporter
): Sequence<ScriptDefinition> {
    val classpath = classLoader.getResources(MANIFEST_RESOURCE_NAME).asSequence().mapNotNull {
        try {
            File(it.toURI()).takeIf(File::exists)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
    val classpathWithLoader = SimpleClasspathWithClassLoader(classpath.toList(), classLoader)
    return scriptTemplatesDiscoverySequence(classpathWithLoader, hostConfiguration, messageReporter)
}

fun discoverScriptTemplatesInClasspath(
    classpath: List<File>,
    baseClassLoader: ClassLoader?,
    hostConfiguration: ScriptingHostConfiguration,
    messageReporter: MessageReporter
): Sequence<ScriptDefinition> {
    // TODO: try to find a way to reduce classpath (and classloader) to minimal one needed to load script definition and its dependencies
    val classpathWithLoader = LazyClasspathWithClassLoader(baseClassLoader) { classpath }

    return scriptTemplatesDiscoverySequence(classpathWithLoader, hostConfiguration, messageReporter)
}

private fun scriptTemplatesDiscoverySequence(
    classpathWithLoader: ClasspathWithClassLoader,
    hostConfiguration: ScriptingHostConfiguration,
    messageReporter: MessageReporter
): Sequence<ScriptDefinition> {
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
                                        hostConfiguration,
                                        messageReporter
                                    )
                                if (notFoundClasses.isNotEmpty()) {
                                    messageReporter(
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
                                .partitionLoadDirDefinitions(dep, classpathWithLoader, hostConfiguration, messageReporter)
                            foundDefinitionClasses.forEach {
                                yield(it)
                            }
                            defferedDefinitionCandidates.addAll(notFoundDefinitions)
                        }
                    }
                    else -> {
                        // assuming that invalid classpath entries will be reported elsewhere anyway, so do not spam user with additional warnings here
                        messageReporter(CompilerMessageSeverity.LOGGING, "Configure scripting: Unknown classpath entry $dep")
                    }
                }
            } catch (e: IOException) {
                messageReporter(
                    CompilerMessageSeverity.STRONG_WARNING, "Configure scripting: unable to process classpath entry $dep: $e"
                )
            }
        }
        var remainingDefinitionCandidates: List<String> = defferedDefinitionCandidates
        for (dep in defferedDirDependencies) {
            if (remainingDefinitionCandidates.isEmpty()) break
            try {
                val (foundDefinitionClasses, notFoundDefinitions) =
                    remainingDefinitionCandidates.partitionLoadDirDefinitions(dep, classpathWithLoader, hostConfiguration, messageReporter)
                foundDefinitionClasses.forEach {
                    yield(it)
                }
                remainingDefinitionCandidates = notFoundDefinitions
            } catch (e: IOException) {
                messageReporter(
                    CompilerMessageSeverity.STRONG_WARNING, "Configure scripting: unable to process classpath entry $dep: $e"
                )
            }
        }
        if (remainingDefinitionCandidates.isNotEmpty()) {
            messageReporter(
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
    hostConfiguration: ScriptingHostConfiguration,
    messageReporter: MessageReporter
): Sequence<ScriptDefinition> =
    if (scriptTemplates.isEmpty()) emptySequence()
    else sequence {
        // trying the direct classloading from baseClassloader first, since this is the most performant variant
        val (initialLoadedDefinitions, initialNotFoundTemplates) = scriptTemplates.partitionMapNotNull {
            loadScriptDefinition(
                baseClassLoader,
                it,
                hostConfiguration,
                messageReporter
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
                            remainingTemplates.partitionLoadJarDefinitions(jar, classpathWithLoader, hostConfiguration, messageReporter)
                        }
                    }
                    dep.isDirectory -> {
                        remainingTemplates.partitionLoadDirDefinitions(dep, classpathWithLoader, hostConfiguration, messageReporter)
                    }
                    else -> {
                        // assuming that invalid classpath entries will be reported elsewhere anyway, so do not spam user with additional warnings here
                        messageReporter(CompilerMessageSeverity.LOGGING, "Configure scripting: Unknown classpath entry $dep")
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
                messageReporter(
                    CompilerMessageSeverity.STRONG_WARNING,
                    "Configure scripting: unable to process classpath entry $dep: $e"
                )
            }
        }

        if (remainingTemplates.isNotEmpty()) {
            messageReporter(
                CompilerMessageSeverity.STRONG_WARNING,
                "Configure scripting: unable to find script definition classes: ${remainingTemplates.joinToString(", ")}"
            )
        }
    }

private data class DefinitionsLoadPartitionResult(
    val loaded: List<ScriptDefinition>,
    val notFound: List<String>
)

private inline fun List<String>.partitionLoadDefinitions(
    classpathWithLoader: ClasspathWithClassLoader,
    hostConfiguration: ScriptingHostConfiguration,
    noinline messageReporter: MessageReporter,
    getBytes: (String) -> ByteArray?
): DefinitionsLoadPartitionResult {
    val loaded = ArrayList<ScriptDefinition>()
    val notFound = ArrayList<String>()
    for (definitionName in this) {
        val classBytes = getBytes(definitionName)
        val definition = classBytes?.let {
            loadScriptDefinition(
                it,
                definitionName,
                classpathWithLoader,
                hostConfiguration,
                messageReporter
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
    hostConfiguration: ScriptingHostConfiguration,
    messageReporter: MessageReporter
): DefinitionsLoadPartitionResult = partitionLoadDefinitions(classpathWithLoader, hostConfiguration, messageReporter) { definitionName ->
    jar.getJarEntry("${definitionName.replace('.', '/')}.class")?.let { jar.getInputStream(it).readBytes() }
}

private fun List<String>.partitionLoadDirDefinitions(
    dir: File,
    classpathWithLoader: ClasspathWithClassLoader,
    hostConfiguration: ScriptingHostConfiguration,
    messageReporter: MessageReporter
): DefinitionsLoadPartitionResult = partitionLoadDefinitions(classpathWithLoader, hostConfiguration, messageReporter) { definitionName ->
    File(dir, "${definitionName.replace('.', '/')}.class").takeIf { it.exists() && it.isFile }?.readBytes()
}

private fun loadScriptDefinition(
    templateClassBytes: ByteArray,
    templateClassName: String,
    classpathWithLoader: ClasspathWithClassLoader,
    hostConfiguration: ScriptingHostConfiguration,
    messageReporter: MessageReporter
): ScriptDefinition? {
    val anns = loadAnnotationsFromClass(templateClassBytes)
    for (ann in anns) {
        var def: ScriptDefinition? = null
        if (ann.name == KotlinScript::class.simpleName) {
            def = LazyScriptDefinitionFromDiscoveredClass(
                hostConfiguration,
                anns,
                templateClassName,
                classpathWithLoader.classpath,
                messageReporter
            )
        } else if (ann.name == ScriptTemplateDefinition::class.simpleName) {
            val templateClass = classpathWithLoader.classLoader.loadClass(templateClassName).kotlin
            def = ScriptDefinition.FromLegacy(
                hostConfiguration,
                KotlinScriptDefinitionFromAnnotatedTemplate(
                    templateClass,
                    hostConfiguration[ScriptingHostConfiguration.getEnvironment]?.invoke().orEmpty(),
                    classpathWithLoader.classpath
                )
            )
        }
        if (def != null) {
            messageReporter(
                CompilerMessageSeverity.LOGGING,
                "Configure scripting: Added template $templateClassName from ${classpathWithLoader.classpath}"
            )
            return def
        }
    }
    messageReporter(
        CompilerMessageSeverity.STRONG_WARNING,
        "Configure scripting: $templateClassName is not marked with any known kotlin script annotation"
    )
    return null
}

private fun loadScriptDefinition(
    classLoader: ClassLoader,
    template: String,
    hostConfiguration: ScriptingHostConfiguration,
    messageReporter: MessageReporter
): ScriptDefinition? {
    try {
        val cls = classLoader.loadClass(template)
        val def =
            if (cls.annotations.firstIsInstanceOrNull<KotlinScript>() != null) {
                ScriptDefinition.FromTemplate(hostConfiguration, cls.kotlin, ScriptDefinition::class)
            } else {
                ScriptDefinition.FromLegacyTemplate(hostConfiguration, cls.kotlin)
            }
        messageReporter(
            CompilerMessageSeverity.INFO,
            "Added script definition $template to configuration: name = ${def.name}"
        )
        return def
    } catch (ex: ClassNotFoundException) {
        // not found - not an error, return null
    } catch (ex: Exception) {
        // other exceptions - might be an error
        messageReporter(
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
