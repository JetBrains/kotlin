/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.utils.graalvm.BundledCompilerPlugins

object NativeImagePluginDirectives : SimpleDirectivesContainer() {
    val COMPILER_PLUGIN by stringDirective(
        description = """
            Usage: // COMPILER_PLUGIN: kotlin-allopen-compiler-plugin.jar annotation=MyOpen
            Declares a plugin compiler (with options) to load.
    """.trimIndent(),
        multiLine = true,
    )

    val COMPILER_PLUGIN_ORDER by stringDirective(
        description = """
            Usage: // COMPILER_PLUGIN_ORDER: org.jetbrains.kotlin.noarg>org.jetbrains.kotlin.allopen
            Declares an execution-order constraint between plugins
    """.trimIndent(),
        multiLine = true,
    )

    val EXPECTED_PLUGIN_ORDER by stringDirective(
        description = """
            Usage: // EXPECTED_PLUGIN_ORDER: org.jetbrains.kotlin.noarg org.jetbrains.kotlin.allopen
            The plugin ids in the order they are expected to be loaded
    """.trimIndent()
    )
}

data class PluginSpec(
    val pluginId: String,
    val jarName: String,
    val options: List<String>,
)

private val WHITESPACE = Regex("""\s+""")

fun RegisteredDirectives.pluginSpecs(): List<PluginSpec> =
    this[NativeImagePluginDirectives.COMPILER_PLUGIN].mapNotNull { entry ->
        val tokens = entry.split(WHITESPACE).filter { it.isNotBlank() }
        val jarName = tokens.firstOrNull() ?: return@mapNotNull null
        val options = tokens.drop(1).flatMap { it.split(",") }.filter { it.isNotBlank() }
        val pluginId = BundledCompilerPlugins.lookupByClasspathEntry(jarName)?.pluginId ?: return@mapNotNull null
        PluginSpec(pluginId, jarName, options)
    }

fun RegisteredDirectives.pluginOrderConstraints(): List<String> =
    this[NativeImagePluginDirectives.COMPILER_PLUGIN_ORDER]

fun RegisteredDirectives.expectedPluginOrder(): List<String> =
    this[NativeImagePluginDirectives.EXPECTED_PLUGIN_ORDER]
