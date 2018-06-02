/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import java.io.File
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

object ScriptingConfigurationKeys {
    val SCRIPT_DEFINITIONS: CompilerConfigurationKey<List<String>> =
        CompilerConfigurationKey.create("Script definition classes")

    val SCRIPT_DEFINITIONS_CLASSPATH: CompilerConfigurationKey<List<File>> =
        CompilerConfigurationKey.create("Additional classpath for the script definitions")

    val DISABLE_SCRIPT_DEFINITIONS_FROM_CLSSPATH_OPTION: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("Do not extract script definitions from the compilation classpath")

    val LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION: CompilerConfigurationKey<MutableMap<String, Any?>> =
        CompilerConfigurationKey.create("Script resolver environment")
}

class ScriptingCommandLineProcessor : CommandLineProcessor {
    companion object {
        val SCRIPT_DEFINITIONS_OPTION = CliOption(
            "script-definitions", "<fully qualified class name[,]>", "Script definition classes",
            required = false, allowMultipleOccurrences = true
        )
        val SCRIPT_DEFINITIONS_CLASSPATH_OPTION = CliOption(
            "script-definitions-classpath", "<classpath entry[:]>", "Additional classpath for the script definitions",
            required = false, allowMultipleOccurrences = true
        )
        val DISABLE_SCRIPT_DEFINITIONS_FROM_CLSSPATH_OPTION = CliOption(
            "disable-script-definitions-from-classpath", "true/false", "Do not extract script definitions from the compilation classpath",
            required = false, allowMultipleOccurrences = false
        )
        val LEGACY_SCRIPT_TEMPLATES_OPTION = CliOption(
            "script-templates", "<fully qualified class name[,]>", "Script definition template classes",
            required = false, allowMultipleOccurrences = true
        )
        val LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION = CliOption(
            "script-resolver-environment", "<key=value[,]>",
            "Script resolver environment in key-value pairs (the value could be quoted and escaped)",
            required = false, allowMultipleOccurrences = true
        )

        val PLUGIN_ID = "kotlin.scripting"
    }

    override val pluginId = PLUGIN_ID
    override val pluginOptions =
        listOf(
            SCRIPT_DEFINITIONS_OPTION,
            SCRIPT_DEFINITIONS_CLASSPATH_OPTION,
            DISABLE_SCRIPT_DEFINITIONS_FROM_CLSSPATH_OPTION,
            LEGACY_SCRIPT_TEMPLATES_OPTION,
            LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION
        )

    override fun processOption(option: CliOption, value: String, configuration: CompilerConfiguration) = when (option) {
        SCRIPT_DEFINITIONS_OPTION, LEGACY_SCRIPT_TEMPLATES_OPTION -> {
            val currentDefs = configuration.getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS).toMutableList()
            currentDefs.addAll(value.split(','))
            configuration.put(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS, currentDefs)
        }
        SCRIPT_DEFINITIONS_CLASSPATH_OPTION -> {
            val currentCP = configuration.getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS_CLASSPATH).toMutableList()
            currentCP.addAll(value.split(File.pathSeparatorChar).map(::File))
            configuration.put(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS_CLASSPATH, currentCP)
        }
        DISABLE_SCRIPT_DEFINITIONS_FROM_CLSSPATH_OPTION -> {
            configuration.put(
                ScriptingConfigurationKeys.DISABLE_SCRIPT_DEFINITIONS_FROM_CLSSPATH_OPTION,
                value.takeUnless { it.isBlank() }?.toBoolean() ?: true
            )
        }
        LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION -> {
            val currentEnv = configuration.getMap(ScriptingConfigurationKeys.LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION).toMutableMap()
            // parses key/value pairs in the form <key>=<value>, where
            //   <key> - is a single word (\w+ pattern)
            //   <value> - optionally quoted string with allowed escaped chars (only double-quote and backslash chars are supported)
            // TODO: implement generic unescaping
            val envParseRe = """(\w+)=(?:"([^"\\]*(\\.[^"\\]*)*)"|([^\s]*))""".toRegex()
            val unescapeRe = """\\(["\\])""".toRegex()
            for (envParam in value.split(',')) {
                val match = envParseRe.matchEntire(envParam)
                if (match == null || match.groupValues.size < 4 || match.groupValues[1].isBlank()) {
                    throw CliOptionProcessingException("Unable to parse script-resolver-environment argument $envParam")
                }
                currentEnv[match.groupValues[1]] =
                        match.groupValues.drop(2).firstOrNull { it.isNotEmpty() }?.let { unescapeRe.replace(it, "\$1") }
            }
            configuration.put(ScriptingConfigurationKeys.LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION, currentEnv)
        }
        else -> throw CliOptionProcessingException("Unknown option: ${option.name}")
    }
}
