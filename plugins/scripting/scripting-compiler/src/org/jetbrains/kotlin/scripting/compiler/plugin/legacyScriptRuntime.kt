/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import org.jetbrains.kotlin.cli.common.kotlinPaths
import org.jetbrains.kotlin.cli.jvm.addModularRootIfNotNull
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.cli.jvm.isModularJava
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.baseClass
import kotlin.script.experimental.impl.fromLegacyTemplate
import kotlin.script.experimental.jvm.util.KotlinJars

private const val LEGACY_STANDARD_TEMPLATES_PACKAGE_PREFIX = "kotlin.script.templates.standard."
private const val SCRIPT_RUNTIME_MODULE_NAME = "kotlin.script.runtime"

/**
 * `true` if scripts compiled with this configuration need `kotlin-script-runtime` on the compilation classpath:
 * the configuration comes from a legacy `@ScriptTemplateDefinition` template, or its base class is one of
 * the deprecated standard templates from `kotlin.script.templates.standard`.
 */
val ScriptCompilationConfiguration.requiresLegacyScriptRuntime: Boolean
    get() {
        @Suppress("DEPRECATION")
        if (this[ScriptCompilationConfiguration.fromLegacyTemplate] == true) return true
        return this[ScriptCompilationConfiguration.baseClass]?.typeName?.startsWith(LEGACY_STANDARD_TEMPLATES_PACKAGE_PREFIX) == true
    }

/**
 * Adds `kotlin-script-runtime` to the content roots if one of the eagerly known script definitions is a legacy one.
 * Definitions discovered later get it via [legacyScriptRuntimeToAdd] when they are selected for a script.
 */
internal fun addLegacyScriptRuntimeIfNeeded(configuration: CompilerConfiguration) {
    val definitions = configuration.getList(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS)
    if (definitions.none { it.compilationConfiguration.requiresLegacyScriptRuntime }) return
    val scriptRuntime = configuration.legacyScriptRuntimeToAdd() ?: return
    configuration.addModularRootIfNotNull(configuration.isModularJava(), SCRIPT_RUNTIME_MODULE_NAME, scriptRuntime)
}

/**
 * The `kotlin-script-runtime` jar for a legacy script definition, or `null` if nothing should be added:
 * as with the stdlib, on `-no-stdlib`, or if the runtime is already on the classpath.
 */
internal fun CompilerConfiguration.legacyScriptRuntimeToAdd(): File? {
    if (getBoolean(JVMConfigurationKeys.NO_STDLIB)) return null
    if ((jvmClasspathRoots + jvmModularRoots).any { it.name.startsWith(PathUtil.KOTLIN_JAVA_SCRIPT_RUNTIME_NAME) }) return null
    return kotlinPaths?.scriptRuntimePath?.takeIf { it.exists() } ?: KotlinJars.scriptRuntimeOrNull
}
