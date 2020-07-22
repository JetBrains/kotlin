/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.script

import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.artifacts.KotlinArtifacts

fun loadScriptingPlugin(configuration: CompilerConfiguration) {
    val artifacts = KotlinArtifacts.instance
    val pluginClasspath = listOf(
        artifacts.kotlinScriptingCompiler,
        artifacts.kotlinScriptingCompilerImpl,
        artifacts.kotlinScriptingCommon,
        artifacts.kotlinScriptingJvm
    ).map { it.absolutePath }

    PluginCliParser.loadPluginsSafe(pluginClasspath, null, configuration)
}