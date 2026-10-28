/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.kotlinPaths
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.extensionsStorage
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.cli.plugins.extractPluginClasspathAndOptions
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

// Short ids are limited to KotlinPaths.Jar entries.
// Compose requires runtime setup; Parcelize is Android-specific; Atomicfu needs a runtime transformation.
private val compilerPluginJars = mapOf(
    "serialization" to KotlinPaths.Jar.SerializationPlugin,
    "allopen" to KotlinPaths.Jar.AllOpenPlugin,
    "noarg" to KotlinPaths.Jar.NoArgPlugin,
    "sam-with-receiver" to KotlinPaths.Jar.SamWithReceiver,
    "lombok" to KotlinPaths.Jar.LombokPlugin,
    "power-assert" to KotlinPaths.Jar.PowerAssertPlugin,
    "assignment" to KotlinPaths.Jar.AssignmentPlugin,
)

fun scriptCompilerPluginDistributionJar(id: String): KotlinPaths.Jar? = compilerPluginJars[id]

internal fun collectScriptCompilerPlugins(
    baseCompilerOptions: List<String>,
    sources: List<SourceCode>,
    getRefinedConfiguration: (SourceCode) -> ScriptCompilationConfiguration?,
    messageCollector: MessageCollector,
): List<ScriptCompilerPlugin> {
    val plugins = linkedMapOf<String, ScriptCompilerPlugin>()
    val baseArguments = makeScriptCompilerArguments(baseCompilerOptions)

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
            addPlugin(ScriptCompilerPlugin.fromClasspath(classpath.split(',').map(::File)))
        }
        arguments.pluginConfigurations.withoutPrefix(baseArguments.pluginConfigurations).forEach { configuration ->
            val parsed = extractPluginClasspathAndOptions(configuration)
            addPlugin(
                ScriptCompilerPlugin.fromClasspath(
                    parsed.classpath.map(::File),
                    parsed.options.map { "${it.optionName}=${it.value}" },
                )
            )
        }
    }

    return plugins.values.toList()
}

internal fun CompilerConfiguration.withScriptCompilerPlugins(
    plugins: List<ScriptCompilerPlugin>,
    disposable: Disposable,
    messageCollector: MessageCollector,
): CompilerConfiguration? {
    if (plugins.isEmpty()) return this

    fun reportError(message: String): CompilerConfiguration? {
        messageCollector.report(CompilerMessageSeverity.ERROR, message)
        return null
    }

    val globalRegistrars = getList(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS)
    val globalPluginIds = globalRegistrars.mapTo(mutableSetOf()) { it.pluginId }
    val globalStorage = extensionsStorage ?: CompilerPluginRegistrar.ExtensionStorage()
    val derivedConfiguration = copy().apply {
        put(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, globalRegistrars.toMutableList())
        extensionsStorage = CompilerPluginRegistrar.ExtensionStorage()
    }
    val deltaStorage = derivedConfiguration.extensionsStorage!!
    val pluginsByRegistrar = LinkedHashMap<CompilerPluginRegistrar, ScriptCompilerPlugin>()
    val requestsByPluginId = mutableMapOf<String, ScriptCompilerPlugin>()

    for (plugin in plugins) {
        val distJar = plugin.id?.let { id ->
            scriptCompilerPluginDistributionJar(id) ?: return reportError("Unknown compiler plugin id '$id'.")
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

        val classpathArgument = classpath.joinToString(",") { it.path }
        val pluginOptions = plugin.options
        val pluginArgument = if (pluginOptions.isEmpty()) classpathArgument else "$classpathArgument=${pluginOptions.joinToString(",")}"
        val registrarsBefore = derivedConfiguration.getList(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS).size
        val loadResult = PluginCliParser.loadPluginsSafe(
            emptyList(), emptyList(), listOf(pluginArgument), emptyList(), derivedConfiguration, disposable, null,
        )
        if (loadResult != ExitCode.OK) return null

        val loadedRegistrars = derivedConfiguration.getList(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS).drop(registrarsBefore)
        for (registrar in loadedRegistrars) {
            if (registrar.pluginId in globalPluginIds) {
                if (plugin.options.isNotEmpty()) {
                    return reportError(
                        "Compiler plugin '${registrar.pluginId}' is already loaded globally and cannot be configured from a script."
                    )
                }
                continue
            }

            val previousRequest = requestsByPluginId[registrar.pluginId]
            if (previousRequest != null && previousRequest !== plugin) {
                if (previousRequest.options != plugin.options) {
                    return reportError("Conflicting options for compiler plugin '${registrar.pluginId}'.")
                }
                continue
            }
            requestsByPluginId[registrar.pluginId] = plugin
            pluginsByRegistrar[registrar] = plugin
        }
    }

    val deltaRegistrars = pluginsByRegistrar.keys
    if (deltaRegistrars.isEmpty()) return this

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
    return derivedConfiguration
}

private val ScriptCompilerPlugin.description: String
    get() = id ?: classpath.joinToString(",")

private fun Array<String>.withoutPrefix(prefix: Array<String>): List<String> =
    if (size >= prefix.size && copyOfRange(0, prefix.size).contentEquals(prefix)) drop(prefix.size) else toList()

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
