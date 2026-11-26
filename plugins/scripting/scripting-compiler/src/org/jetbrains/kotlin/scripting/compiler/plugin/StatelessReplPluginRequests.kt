/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import java.io.*
import java.util.*

data class StatelessReplPluginRequest(
    val key: String,
    val pluginId: String,
    val classpath: List<File>,
    val options: List<String>,
)

data class StatelessReplLoadedPlugin(
    val key: String,
    val pluginId: String,
    val options: List<String>,
)

object StatelessReplPluginRequestsCodec {
    private const val MAX_ENTRIES = 10_000
    private const val MISSING_PLUGIN_HEADER = "Compilation requires compiler plugin "
    private const val PLUGIN_CLASSPATH_PREFIX = "-Xplugin="
    private const val PLUGIN_OPTION_PREFIX = "-P plugin:"
    private val missingPluginHeaderRegex = Regex("${Regex.escape(MISSING_PLUGIN_HEADER)}'(.+)' \\(id '([^']+)'\\):")

    /**
     * Renders the error diagnostic reported by a stateless snippet compilation for a requested compiler plugin
     * that is not loaded yet. The header names the request and the plugin id, and it is followed by the legacy
     * CLI plugin arguments (`-Xplugin=<jar>` and `-P plugin:<id>:<option>`) needed to load the plugin, one per line,
     * so the host can extract the request with [extractMissingPluginRequests] and retry the compilation.
     */
    fun missingPluginMessage(request: StatelessReplPluginRequest): String = buildString {
        append("$MISSING_PLUGIN_HEADER'${request.key}' (id '${request.pluginId}'):")
        request.classpath.forEach { append("\n  $PLUGIN_CLASSPATH_PREFIX${it.path}") }
        request.options.forEach { append("\n  $PLUGIN_OPTION_PREFIX${request.pluginId}:$it") }
    }

    /**
     * Extracts the requests from all diagnostics rendered by [missingPluginMessage] found in [message],
     * or returns `null` if the message contains no such diagnostic.
     */
    fun extractMissingPluginRequests(message: String): List<StatelessReplPluginRequest>? {
        val requests = mutableListOf<StatelessReplPluginRequest>()
        val lines = message.lines()
        var index = 0
        while (index < lines.size) {
            val header = missingPluginHeaderRegex.find(lines[index++]) ?: continue
            val key = header.groupValues[1]
            val pluginId = header.groupValues[2]
            val optionPrefix = "$PLUGIN_OPTION_PREFIX$pluginId:"
            val classpath = mutableListOf<File>()
            val options = mutableListOf<String>()
            while (index < lines.size) {
                val line = lines[index].trim()
                when {
                    line.startsWith(PLUGIN_CLASSPATH_PREFIX) -> classpath += File(line.removePrefix(PLUGIN_CLASSPATH_PREFIX))
                    line.startsWith(optionPrefix) -> options += line.removePrefix(optionPrefix)
                    else -> break
                }
                index++
            }
            require(classpath.isNotEmpty()) { "No classpath specified for the requested compiler plugin '$key'" }
            requests += StatelessReplPluginRequest(key, pluginId, classpath, options)
        }
        return requests.takeIf { it.isNotEmpty() }
    }

    fun encodeLoadedPluginOption(key: String, pluginId: String, options: List<String>): String {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { output ->
            output.writeUTF(key)
            output.writeUTF(pluginId)
            output.writeInt(options.size)
            options.forEach(output::writeUTF)
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray())
    }

    fun decodeLoadedPluginOption(value: String): StatelessReplLoadedPlugin =
        DataInputStream(Base64.getUrlDecoder().decode(value).inputStream()).use { input ->
            val key = input.readUTF()
            val pluginId = input.readUTF()
            val count = input.readInt().also { require(it in 0..MAX_ENTRIES) { "Invalid loaded plugin option count: $it" } }
            StatelessReplLoadedPlugin(key, pluginId, List(count) { input.readUTF() })
        }
}
