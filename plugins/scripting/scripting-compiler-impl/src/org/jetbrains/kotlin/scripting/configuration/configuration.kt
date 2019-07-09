/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.configuration

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.scripting.definitions.loadScriptTemplatesFromClasspath
import org.jetbrains.kotlin.scripting.definitions.reporter
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

const val KOTLIN_SCRIPTING_PLUGIN_ID = "kotlin.scripting"

fun configureScriptDefinitions(
    scriptTemplates: List<String>,
    configuration: CompilerConfiguration,
    baseClassloader: ClassLoader,
    messageCollector: MessageCollector,
    hostConfiguration: ScriptingHostConfiguration
) {
    // TODO: consider using escaping to allow kotlin escaped names in class names
    val templatesFromClasspath = loadScriptTemplatesFromClasspath(
        scriptTemplates, configuration.jvmClasspathRoots, emptyList(), baseClassloader, hostConfiguration, messageCollector.reporter
    )
    configuration.addAll(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS, templatesFromClasspath.toList())
}

fun makeHostConfiguration(project: Project, configuration: CompilerConfiguration): ScriptingHostConfiguration =
    ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {
        // TODO: add jdk path and other params if needed
    }