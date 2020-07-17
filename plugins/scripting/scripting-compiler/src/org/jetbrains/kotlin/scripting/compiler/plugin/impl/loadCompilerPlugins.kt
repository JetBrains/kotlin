/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.jvm.plugins.processCompilerPluginsOptions
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingCommandLineProcessor
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingCompilerConfigurationComponentRegistrar
import kotlin.script.experimental.jvm.util.forAllMatchingFiles

private const val SCRIPT_COMPILATION_DISABLE_PLUGINS_PROPERTY = "script.compilation.disable.plugins"
private const val SCRIPT_COMPILATION_DISABLE_COMMANDLINE_PROCESSORS_PROPERTY = "script.compilation.disable.commandline.processors"

private val scriptCompilationDisabledPlugins =
    listOf(
        ScriptingCompilerConfigurationComponentRegistrar::class.java.name
    )

private val scriptCompilationDisabledCommandlineProcessors =
    listOf(
        ScriptingCommandLineProcessor::class.java.name
    )

internal fun CompilerConfiguration.loadPlugins() {
    val classLoader = CompilerConfiguration::class.java.classLoader
    val registrars =
        classLoader.loadServices<ComponentRegistrar>(scriptCompilationDisabledPlugins, SCRIPT_COMPILATION_DISABLE_PLUGINS_PROPERTY)
    addAll(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS, registrars)
}

internal fun CompilerConfiguration.processPluginsCommandLine(arguments: K2JVMCompilerArguments) {
    val classLoader = CompilerConfiguration::class.java.classLoader
    val pluginOptions = arguments.pluginOptions?.asIterable() ?: emptyList()

    val commandLineProcessors =
        classLoader.loadServices<CommandLineProcessor>(
            scriptCompilationDisabledCommandlineProcessors, SCRIPT_COMPILATION_DISABLE_COMMANDLINE_PROCESSORS_PROPERTY
        )
    processCompilerPluginsOptions(this, pluginOptions, commandLineProcessors)
}

private inline fun <reified Service : Any> ClassLoader.loadServices(disabled: List<String>, disablingProperty: String): List<Service> {
    val disabledServiceNames = disabled.toHashSet()
    System.getProperty(disablingProperty)?.let {
        it.split(',', ';', ' ').forEach { name ->
            disabledServiceNames.add(name.trim())
        }
    }
    return loadServices {
        !disabledServiceNames.contains(it) && !disabledServiceNames.contains(it.substringAfterLast('.'))
    }
}

private const val SERVICE_DIRECTORY_LOCATION = "META-INF/services/"

private inline fun <reified Service : Any> ClassLoader.loadServices(isEnabled: (String) -> Boolean): List<Service> {
    val registrarsNames = HashSet<String>()
    val serviceFileName = SERVICE_DIRECTORY_LOCATION + Service::class.java.name

    forAllMatchingFiles(serviceFileName, serviceFileName) { name, stream ->
        stream.reader().useLines {
            it.mapNotNullTo(registrarsNames) { parseServiceFileLine(name, it) }
        }
    }

    return registrarsNames.mapNotNull { if (isEnabled(it)) (loadClass(it).newInstance() as Service) else null }
}

private fun parseServiceFileLine(location: String, line: String): String? {
    val actualLine = line.substringBefore('#').trim().takeIf { it.isNotEmpty() } ?: return null
    actualLine.forEachIndexed { index: Int, c: Char ->
        val isValid = if (index == 0) Character.isJavaIdentifierStart(c) else Character.isJavaIdentifierPart(c) || c == '.'
        if (!isValid) {
            val errorText = "Invalid Java identifier: $line"
            throw RuntimeException("Error loading services from $location : $errorText")
        }
    }
    return actualLine
}
