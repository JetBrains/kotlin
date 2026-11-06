/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import java.io.File
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.asErrorDiagnostics

class StatelessReplCompilerPlugins private constructor(
    private val byKey: Map<String, StatelessReplPluginRequest>,
) {
    fun merge(requests: List<StatelessReplPluginRequest>): ResultWithDiagnostics<StatelessReplCompilerPlugins> {
        val merged = LinkedHashMap(byKey)
        for (request in requests) {
            val sameKey = merged[request.key]
            if (sameKey != null) {
                if (sameKey.pluginId != request.pluginId || sameKey.options != request.options) {
                    return conflict(sameKey.description())
                }
                continue
            }

            val samePlugin = merged.values.firstOrNull { it.pluginId == request.pluginId }
            if (samePlugin != null) {
                if (samePlugin.options != request.options) return conflict(request.pluginId)
                continue
            }

            merged[request.key] = request
        }
        return ResultWithDiagnostics.Success(StatelessReplCompilerPlugins(merged))
    }

    fun cliArguments(): List<String> = buildList {
        byKey.values.flatMap { it.classpath }.distinct().forEach { classpath ->
            add("-Xplugin=${classpath.path}")
        }
        byKey.values.forEach { [_, pluginId, _, options] ->
            options.forEach { option ->
                val optionSeparator = option.indexOf('=')
                require(optionSeparator > 0) { "Invalid compiler plugin option '$option'." }
                val optionName = option.substring(0, optionSeparator)
                val optionValue = option.substring(optionSeparator + 1)
                add("-P")
                add("plugin:$pluginId:$optionName=$optionValue")
            }
        }
    }

    fun loadedKeys(): List<String> = byKey.keys.toList()

    fun entries(): List<StatelessReplPluginRequest> = byKey.values.toList()

    fun loadedKeyOptions(): List<String> = byKey.values.map { [key, pluginId, _, options] ->
        StatelessReplPluginRequestsCodec.encodeLoadedPluginOption(key, pluginId, options)
    }

    private fun StatelessReplPluginRequest.description(): String {
        val classpathKey = classpath.joinToString(File.pathSeparator) { it.absoluteFile.normalize().path }
        return if (key == classpathKey) classpath.joinToString(",") { it.path } else key
    }

    private fun conflict(pluginDescription: String): ResultWithDiagnostics.Failure =
        ResultWithDiagnostics.Failure(
            "Conflicting options for compiler plugin '$pluginDescription'.".asErrorDiagnostics()
        )

    companion object {
        val EMPTY = StatelessReplCompilerPlugins(emptyMap())
    }
}
