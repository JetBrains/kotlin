/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.test

import org.jetbrains.kotlin.scripting.compiler.plugin.StatelessReplCompilerPlugins
import org.jetbrains.kotlin.scripting.compiler.plugin.StatelessReplLoadedPlugin
import org.jetbrains.kotlin.scripting.compiler.plugin.StatelessReplPluginRequest
import org.jetbrains.kotlin.scripting.compiler.plugin.StatelessReplPluginRequestsCodec
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.script.experimental.api.valueOrThrow
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class StatelessReplCompilerPluginsTest {
    @Test
    fun testMergeDeduplicatesEqualRequests() {
        val request = request()

        val merged = StatelessReplCompilerPlugins.EMPTY.merge(listOf(request, request)).valueOrThrow()

        assertEquals(1, merged.loadedKeys().size)
        assertEquals(1, merged.cliArguments().count { it.startsWith("-Xplugin=") })
        assertEquals(1, merged.loadedKeyOptions().size)
    }

    @Test
    fun testMergeDeduplicatesSamePluginAcrossRequestKeys() {
        val request = request()
        val aliasRequest = request.copy(key = "serialization")

        val merged = StatelessReplCompilerPlugins.EMPTY.merge(listOf(request, aliasRequest)).valueOrThrow()

        assertEquals(listOf(request.key), merged.loadedKeys())
        assertEquals(1, merged.cliArguments().count { it.startsWith("-Xplugin=") })
    }

    @Test
    fun testMergeRejectsConflictingOptions() {
        val request = request()
        val conflict = request.copy(options = listOf("disableIntrinsic=false"))

        val failure = assertFailsWith<RuntimeException> {
            StatelessReplCompilerPlugins.EMPTY.merge(listOf(request, conflict)).valueOrThrow()
        }

        assertContains(
            failure.message.orEmpty(),
            "Conflicting options for compiler plugin '${request.classpath.joinToString(",") { it.path }}'.",
        )
    }

    @Test
    fun testCliArgumentsUseCompatibleLegacySyntax() {
        val classpath = listOf(File("compiler-plugins/serialization.jar"), File("compiler-plugins/dependency.jar"))
        val request = StatelessReplPluginRequest(
            key = "serialization",
            pluginId = "org.jetbrains.kotlinx.serialization",
            classpath = classpath,
            options = listOf("disableIntrinsic=true"),
        )

        val arguments = StatelessReplCompilerPlugins.EMPTY.merge(listOf(request)).valueOrThrow().cliArguments()

        assertEquals(
            listOf(
                "-Xplugin=${classpath[0].path}",
                "-Xplugin=${classpath[1].path}",
                "-P",
                "plugin:org.jetbrains.kotlinx.serialization:disableIntrinsic=true",
            ),
            arguments,
        )
    }

    @Test
    fun testRequestAndLoadedPluginCodecsRoundTrip() {
        val request = request()

        val otherRequest = request.copy(key = "other", pluginId = "other.plugin", options = emptyList())

        val message = StatelessReplPluginRequestsCodec.missingPluginMessage(request)
        assertEquals(
            """
            Compilation requires compiler plugin '${request.key}' (id 'org.jetbrains.kotlinx.serialization'):
              -Xplugin=${request.classpath.single().path}
              -P plugin:org.jetbrains.kotlinx.serialization:disableIntrinsic=true
            """.trimIndent(),
            message,
        )

        // Hosts receive the diagnostics rendered with prefixes, possibly several of them per compilation.
        assertEquals(listOf(request), StatelessReplPluginRequestsCodec.extractMissingPluginRequests("error: $message"))
        assertEquals(
            listOf(request, otherRequest),
            StatelessReplPluginRequestsCodec.extractMissingPluginRequests(
                "ERROR: $message\nERROR: " + StatelessReplPluginRequestsCodec.missingPluginMessage(otherRequest)
            )
        )
        assertNull(StatelessReplPluginRequestsCodec.extractMissingPluginRequests("error: Compilation requires compiler plugins: x"))

        val loadedPlugin = StatelessReplLoadedPlugin(request.key, request.pluginId, request.options)
        val encoded = StatelessReplPluginRequestsCodec.encodeLoadedPluginOption(
            loadedPlugin.key,
            loadedPlugin.pluginId,
            loadedPlugin.options,
        )
        assertEquals(loadedPlugin, StatelessReplPluginRequestsCodec.decodeLoadedPluginOption(encoded))
    }

    private fun request(): StatelessReplPluginRequest {
        val classpath = listOf(File("compiler-plugins/serialization.jar").absoluteFile.normalize())
        val key = classpath.joinToString(File.pathSeparator) { it.absoluteFile.normalize().path }
        return StatelessReplPluginRequest(key, "org.jetbrains.kotlinx.serialization", classpath, listOf("disableIntrinsic=true"))
    }
}
