/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.extensionsStorage
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.cli.plugins.extractPluginClasspathAndOptions
import org.jetbrains.kotlin.cli.common.kotlinPaths
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.ExtensionPointDescriptor
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.utils.KotlinPaths
import java.io.File
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptCompilerPlugin
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.compilerPlugins

private val compilerPluginJars = mapOf(
    "serialization" to KotlinPaths.Jar.SerializationPlugin,
    "allopen" to KotlinPaths.Jar.AllOpenPlugin,
    "noarg" to KotlinPaths.Jar.NoArgPlugin,
    "sam-with-receiver" to KotlinPaths.Jar.SamWithReceiver,
    "lombok" to KotlinPaths.Jar.LombokPlugin,
    "power-assert" to KotlinPaths.Jar.PowerAssertPlugin,
    "assignment" to KotlinPaths.Jar.AssignmentPlugin,
)

private val compilerPluginIdsByJarBaseName = compilerPluginJars.entries.associate { it.value.baseName to it.key }

internal fun collectScriptCompilerPlugins(
    baseCompilerOptions: List<String>,
    sources: List<SourceCode>,
    getRefinedConfiguration: (SourceCode) -> ScriptCompilationConfiguration?,
    messageCollector: MessageCollector,
): List<ScriptCompilerPlugin> {
    val plugins = linkedMapOf<String, ScriptCompilerPlugin>()
    val pluginOptions = mutableListOf<String>()
    val baseArguments = makeScriptCompilerArguments(emptyList())

    fun addPlugin(plugin: ScriptCompilerPlugin) {
        val key = plugin.id ?: plugin.classpath.joinToString(File.pathSeparator) { it.absoluteFile.normalize().path }
        val previous = plugins[key]
        if (previous == null) {
            plugins[key] = plugin
        } else if (previous.options != plugin.options) {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Conflicting options for compiler plugin '${plugin.id ?: plugin.classpath.joinToString()}'.",
            )
        }
    }

    sources.forEach { source ->
        val refinedConfiguration = getRefinedConfiguration(source) ?: return@forEach
        refinedConfiguration[ScriptCompilationConfiguration.compilerPlugins].orEmpty().forEach(::addPlugin)

        val compilerOptions = refinedConfiguration[ScriptCompilationConfiguration.compilerOptions].orEmpty()
        val optionsDelta = refinedCompilerOptionsDelta(baseCompilerOptions, compilerOptions)
        if (optionsDelta.isEmpty()) return@forEach

        val arguments = makeScriptCompilerArguments(optionsDelta)
        arguments.pluginClasspaths.withoutPrefix(baseArguments.pluginClasspaths).forEach { classpath ->
            addPlugin(ScriptCompilerPlugin(id = null, classpath = classpath.split(',').map(::File)))
        }
        arguments.pluginConfigurations.withoutPrefix(baseArguments.pluginConfigurations).forEach { configuration ->
            val parsed = extractPluginClasspathAndOptions(configuration)
            addPlugin(
                ScriptCompilerPlugin(
                    id = null,
                    classpath = parsed.classpath.map(::File),
                    options = parsed.options.map { "${it.optionName}=${it.value}" },
                )
            )
        }
        pluginOptions.addAll(arguments.pluginOptions.withoutPrefix(baseArguments.pluginOptions))
    }

    return buildList {
        addAll(plugins.values)
        if (pluginOptions.isNotEmpty()) add(ScriptCompilerPlugin(id = null, classpath = emptyList(), options = pluginOptions))
    }
}

internal fun CompilerConfiguration.withScriptCompilerPlugins(
    plugins: List<ScriptCompilerPlugin>,
    baseCompilerOptions: List<String>,
    disposable: Disposable,
    messageCollector: MessageCollector,
): CompilerConfiguration? {
    val legacyPluginOptions = plugins.filter { it.id == null && it.classpath.isEmpty() }.flatMap { it.options }
    val requestedPlugins = plugins.filterNot { it.id == null && it.classpath.isEmpty() }
    val globalRegistrars = getList(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS)
    val globalPluginIds = globalRegistrars.map { it.pluginId }.toSet()
    val basePluginOptions = BasePluginOptions(baseCompilerOptions)
    val verifiedPluginOptions = mutableSetOf<String>()

    fun reportError(message: String): CompilerConfiguration? {
        messageCollector.report(CompilerMessageSeverity.ERROR, message)
        return null
    }

    fun reportIgnoredPluginOptions(options: List<String>) {
        val ignoredOptions = options - verifiedPluginOptions
        if (ignoredOptions.isEmpty()) return
        messageCollector.report(
            CompilerMessageSeverity.STRONG_WARNING,
            "The following compiler arguments are ignored when configured from refinement callbacks: ${ignoredOptions.joinToString(" ")}.",
        )
    }

    // A script may repeat the options of an already loaded plugin, but only when they are known to be equal.
    // Options of plugins loaded outside the script definition (e.g. by the CLI) are not available here.
    fun reconfigurationError(pluginId: String, request: ScriptCompilerPlugin, classpath: List<File>): String? {
        val legacyOptionsOfPlugin = legacyPluginOptions.filter { option ->
            parseLegacyPluginOption(option)?.let { pluginIdsMatch(it.pluginId, pluginId) } == true
        }
        val requestedOptions = request.options + legacyOptionsOfPlugin.map { parseLegacyPluginOption(it)!!.value }
        if (requestedOptions.isEmpty()) return null
        val globalOptions = basePluginOptions.optionsOf(pluginId, classpath)
            ?: return "Compiler plugin '$pluginId' is already loaded globally and its options cannot be verified; remove the options from the script."
        if (requestedOptions.sorted() != globalOptions.sorted()) {
            return "Compiler plugin '$pluginId' is already loaded globally with different options and cannot be reconfigured by a script."
        }
        verifiedPluginOptions += legacyOptionsOfPlugin
        return null
    }

    val resolvedPlugins = mutableListOf<ResolvedScriptCompilerPlugin>()
    for (plugin in requestedPlugins) {
        val shortId = plugin.id ?: plugin.classpath.firstNotNullOfOrNull(::shortPluginIdFromClasspath)
        val distJar = plugin.id?.let { compilerPluginJars[it] }
        if (plugin.id != null && distJar == null) {
            return reportError("Unknown compiler plugin id '${plugin.id}'.")
        }

        val globallyLoadedId = shortId?.let { id -> globalPluginIds.firstOrNull { pluginIdsMatch(id, it) } }
        if (globallyLoadedId != null) {
            reconfigurationError(globallyLoadedId, plugin, plugin.classpath.map { it.absoluteFile.normalize() })
                ?.let { return reportError(it) }
            continue
        }

        val classpath = if (distJar != null) {
            val paths = kotlinPaths
                ?: return reportError("Compiler plugin id '${plugin.id}' cannot be resolved by this host. Use a jar path instead.")
            listOf(paths.jar(distJar))
        } else {
            plugin.classpath
        }.map { it.absoluteFile.normalize() }

        for (jar in classpath) {
            if (!jar.isFile) return reportError("Compiler plugin jar '$jar' does not exist or is not a file.")
        }
        resolvedPlugins += ResolvedScriptCompilerPlugin(plugin, classpath, shortId)
    }

    val uniquePlugins = linkedMapOf<String, ResolvedScriptCompilerPlugin>()
    for (plugin in resolvedPlugins) {
        val key = plugin.shortId ?: plugin.classpath.joinToString(File.pathSeparator) { it.path }
        val previous = uniquePlugins[key]
        if (previous == null) {
            uniquePlugins[key] = plugin
        } else if (previous.request.options != plugin.request.options) {
            return reportError("Conflicting options for compiler plugin '${plugin.description}'.")
        }
    }
    resolvedPlugins.clear()
    resolvedPlugins.addAll(uniquePlugins.values)

    if (resolvedPlugins.isEmpty()) {
        reportIgnoredPluginOptions(legacyPluginOptions)
        return this
    }

    val legacyOptionsByPlugin = resolvedPlugins.associateWith { mutableListOf<String>() }
    val unmatchedLegacyOptions = mutableListOf<String>()
    for (option in legacyPluginOptions) {
        val parsedOption = parseLegacyPluginOption(option)
        val matchingPlugin = if (parsedOption == null || parsedOption.pluginId in globalPluginIds) {
            null
        } else {
            resolvedPlugins.filter { it.matches(parsedOption.pluginId) }.singleOrNull()
                ?: resolvedPlugins.singleOrNull()
        }
        if (matchingPlugin == null || parsedOption == null) unmatchedLegacyOptions += option
        else legacyOptionsByPlugin.getValue(matchingPlugin) += parsedOption.value
    }

    val globalStorage = extensionsStorage ?: CompilerPluginRegistrar.ExtensionStorage()
    val derivedConfiguration = copy().apply {
        put(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, globalRegistrars.toMutableList())
        extensionsStorage = CompilerPluginRegistrar.ExtensionStorage()
    }
    val deltaStorage = derivedConfiguration.extensionsStorage!!
    // Plugins are loaded one by one, so that every loaded registrar is attributed to its request.
    val pluginsByRegistrar = LinkedHashMap<CompilerPluginRegistrar, ResolvedScriptCompilerPlugin>()
    for (plugin in resolvedPlugins) {
        val classpath = plugin.classpath.joinToString(",") { it.path }
        val options = plugin.request.options + legacyOptionsByPlugin.getValue(plugin)
        val modernStyleString = if (options.isEmpty()) classpath else "$classpath=${options.joinToString(",")}"
        val registrarsBefore = derivedConfiguration.getList(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS).size
        val loadResult = PluginCliParser.loadPluginsSafe(
            emptyList(), emptyList(), listOf(modernStyleString), emptyList(), derivedConfiguration, disposable, null,
        )
        if (loadResult != ExitCode.OK) return null
        derivedConfiguration.getList(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS).drop(registrarsBefore)
            .forEach { pluginsByRegistrar[it] = plugin }
    }

    val duplicateRegistrars = pluginsByRegistrar.keys.filter { it.pluginId in globalPluginIds }
    for (registrar in duplicateRegistrars) {
        val plugin = pluginsByRegistrar.getValue(registrar)
        reconfigurationError(registrar.pluginId, plugin.request, plugin.classpath)?.let { return reportError(it) }
    }
    val deltaRegistrars = pluginsByRegistrar.keys - duplicateRegistrars.toSet()
    if (deltaRegistrars.isEmpty()) {
        reportIgnoredPluginOptions(unmatchedLegacyOptions)
        return this
    }

    val unsupportedRegistrar = deltaRegistrars.firstOrNull { !it.supportsK2 }
    if (unsupportedRegistrar != null) {
        return reportError(
            "Compiler plugin '${pluginsByRegistrar.getValue(unsupportedRegistrar).description}' " +
                    "does not support K2 and cannot be loaded from a script."
        )
    }

    for (registrar in deltaRegistrars) {
        val pluginName = pluginsByRegistrar.getValue(registrar).description
        try {
            with(registrar) { deltaStorage.registerExtensions(derivedConfiguration) }
        } catch (failure: Throwable) {
            deltaStorage.disposables.forEach { it.dispose() }
            return reportError("Compiler plugin '$pluginName' failed while registering extensions: ${failure.message ?: failure.javaClass.name}.")
        }
        val projectExtension = deltaStorage.registeredExtensions.keys.firstOrNull { it is ProjectExtensionDescriptor<*> }
        if (projectExtension != null) {
            deltaStorage.disposables.forEach { it.dispose() }
            return reportError(
                "Compiler plugin '$pluginName' registers project extension point '${projectExtension.name}' and cannot be loaded from a script."
            )
        }
    }

    val combinedStorage = CompilerPluginRegistrar.ExtensionStorage().apply {
        copyExtensionsFrom(globalStorage)
        copyExtensionsFrom(deltaStorage)
        deltaStorage.disposables.forEach(::registerDisposable)
    }
    derivedConfiguration.extensionsStorage = combinedStorage
    derivedConfiguration.put(
        CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS,
        (globalRegistrars + deltaRegistrars).toMutableList(),
    )

    reportIgnoredPluginOptions(unmatchedLegacyOptions)
    return derivedConfiguration
}

private data class ResolvedScriptCompilerPlugin(
    val request: ScriptCompilerPlugin,
    val classpath: List<File>,
    val shortId: String?,
) {
    val description: String
        get() = request.id ?: classpath.joinToString(",")

    fun matches(pluginId: String): Boolean = shortId?.let { pluginIdsMatch(it, pluginId) } == true
}

/**
 * Plugin options from the script definition's static compiler options, in the `name=value` form of the script requests.
 */
private class BasePluginOptions(baseCompilerOptions: List<String>) {
    private class Entry(val classpath: List<File>, val shortId: String?, val options: List<String>)

    private val entries: List<Entry>
    private val legacyOptions: List<ParsedLegacyPluginOption>

    init {
        val arguments = makeScriptCompilerArguments(baseCompilerOptions)
        val legacyEntries = arguments.pluginClasspaths.orEmpty().map { classpath ->
            createEntry(classpath.split(','), emptyList())
        }
        val modernEntries = arguments.pluginConfigurations.orEmpty().map { configuration ->
            val parsed = extractPluginClasspathAndOptions(configuration)
            createEntry(parsed.classpath, parsed.options.map { "${it.optionName}=${it.value}" })
        }
        entries = legacyEntries + modernEntries
        legacyOptions = arguments.pluginOptions.orEmpty().mapNotNull(::parseLegacyPluginOption)
    }

    /** Returns `null` when the plugin does not come from the base compiler options, so its options are unknown. */
    fun optionsOf(pluginId: String, classpath: List<File>): List<String>? {
        val matchingEntries = entries.filter { entry ->
            (classpath.isNotEmpty() && entry.classpath == classpath) || entry.shortId?.let { pluginIdsMatch(it, pluginId) } == true
        }
        if (matchingEntries.isEmpty()) return null
        return matchingEntries.flatMap { it.options } + legacyOptions.filter { it.pluginId == pluginId }.map { it.value }
    }

    private fun createEntry(classpath: List<String>, options: List<String>): Entry {
        val files = classpath.map { File(it).absoluteFile.normalize() }
        return Entry(files, files.firstNotNullOfOrNull(::shortPluginIdFromClasspath), options)
    }
}

private data class ParsedLegacyPluginOption(val pluginId: String, val value: String)

private fun parseLegacyPluginOption(option: String): ParsedLegacyPluginOption? {
    if (!option.startsWith("plugin:")) return null
    val body = option.removePrefix("plugin:")
    val pluginId = body.substringBefore(':')
    val value = body.substringAfter(':', missingDelimiterValue = "")
    return if (pluginId.isNotEmpty() && value.isNotEmpty()) ParsedLegacyPluginOption(pluginId, value) else null
}

private fun shortPluginIdFromClasspath(file: File): String? = compilerPluginIdsByJarBaseName[file.nameWithoutExtension]

private fun Array<String>.withoutPrefix(prefix: Array<String>): List<String> =
    if (size >= prefix.size && copyOfRange(0, prefix.size).contentEquals(prefix)) drop(prefix.size) else toList()

private fun pluginIdsMatch(shortId: String, pluginId: String): Boolean {
    val normalizedShortId = shortId.filter(Char::isLetterOrDigit)
    val normalizedPluginId = pluginId.filter(Char::isLetterOrDigit)
    val normalizedPluginName = pluginId.substringAfterLast('.').filter(Char::isLetterOrDigit)
    return normalizedShortId.equals(normalizedPluginName, ignoreCase = true) ||
            normalizedShortId.equals(normalizedPluginId, ignoreCase = true)
}

private fun CompilerPluginRegistrar.ExtensionStorage.copyExtensionsFrom(other: CompilerPluginRegistrar.ExtensionStorage) {
    other.registeredExtensions.forEach { entry ->
        val descriptor = entry.key
        val extensions = entry.value
        extensions.forEach { extension ->
            @Suppress("UNCHECKED_CAST")
            (descriptor as ExtensionPointDescriptor<Any>).registerExtension(extension)
        }
    }
}
