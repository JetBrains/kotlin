/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import org.jetbrains.kotlin.scripting.compiler.plugin.impl.readRefinedCompilationConfiguration
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.baseClass
import kotlin.script.experimental.api.hostConfiguration
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.configurationDependencies
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.util.classpathFromClass

object ReplSnippetConfigurationCodec {

    fun encode(configuration: ScriptCompilationConfiguration): ByteArray {
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { it.writeObject(configuration) }
        return bytes.toByteArray()
    }

    fun decode(bytes: ByteArray): ScriptCompilationConfiguration =
        ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as ScriptCompilationConfiguration }

    fun writeTo(configuration: ScriptCompilationConfiguration, file: File) {
        file.parentFile?.mkdirs()
        file.writeBytes(encode(configuration))
    }

    fun readFrom(file: File): ScriptCompilationConfiguration = decode(file.readBytes())
}

/**
 * The `@KotlinScript` template a configuration is built from, with the classpath to load it and its configuration
 * dependencies from; passed to the compiler via `script-definitions` and `script-definitions-classpath`.
 */
data class ScriptTemplateWithClasspath(val templateClassName: String, val classpath: List<File>)

/**
 * The template [configuration] is built from, or `null` if its base class is not a `@KotlinScript` template.
 */
fun scriptTemplateWithClasspath(configuration: ScriptCompilationConfiguration): ScriptTemplateWithClasspath? {
    val templateClass = configuration[ScriptCompilationConfiguration.baseClass]?.fromClass ?: return null
    if (templateClass.java.getAnnotation(KotlinScript::class.java) == null) return null
    val templateClasspath = classpathFromClass(templateClass) ?: return null
    val hostClasspath = configuration[ScriptCompilationConfiguration.hostConfiguration]
        ?.get(ScriptingHostConfiguration.configurationDependencies).orEmpty()
        .filterIsInstance<JvmDependency>().flatMap { it.classpath }
    val classpath = (templateClasspath + hostClasspath).map { it.absoluteFile }.distinct()
    return ScriptTemplateWithClasspath(templateClass.java.name, classpath)
}

/**
 * The configuration [refined] by the compiler, with the gaps filled from this host-side configuration.
 * The compiler refines the configuration the host has passed to it, so [refined] lacks only the transient
 * properties, e.g. the refinement handlers and the host configuration, which cannot be transported.
 */
fun ScriptCompilationConfiguration.withRefinedFromCompiler(refined: ScriptCompilationConfiguration): ScriptCompilationConfiguration =
    ScriptCompilationConfiguration(this, refined)

/**
 * [withRefinedFromCompiler] with the refined configuration embedded into [snippetClassFile], if there is any.
 */
fun ScriptCompilationConfiguration.withRefinedFromSnippetClass(snippetClassFile: File): ScriptCompilationConfiguration =
    readRefinedCompilationConfiguration(snippetClassFile)?.let { withRefinedFromCompiler(it) } ?: this
