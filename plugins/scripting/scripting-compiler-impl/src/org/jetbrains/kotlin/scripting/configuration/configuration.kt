/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.configuration

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.scripting.definitions.loadScriptTemplatesFromClasspath
import org.jetbrains.kotlin.scripting.definitions.reporter

const val KOTLIN_SCRIPTING_PLUGIN_ID = "kotlin.scripting"

fun configureScriptDefinitions(
    scriptTemplates: List<String>,
    configuration: CompilerConfiguration,
    baseClassloader: ClassLoader,
    messageCollector: MessageCollector,
    scriptResolverEnv: Map<String, Any?>
) {
    // TODO: consider using escaping to allow kotlin escaped names in class names
    val templatesFromClasspath = loadScriptTemplatesFromClasspath(
        scriptTemplates, configuration.jvmClasspathRoots, emptyList(), baseClassloader, scriptResolverEnv, messageCollector.reporter
    )
    configuration.addAll(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS, templatesFromClasspath.toList())
}

