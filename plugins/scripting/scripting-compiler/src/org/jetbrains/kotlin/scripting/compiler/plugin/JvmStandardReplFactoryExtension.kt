/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import com.intellij.core.JavaCoreProjectEnvironment
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.extensions.ReplFactoryExtension
import org.jetbrains.kotlin.cli.common.repl.ReplCompiler
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.scripting.repl.GenericReplCompiler
import java.io.File
import java.net.URLClassLoader

class JvmStandardReplFactoryExtension : ReplFactoryExtension {

    override fun makeReplCompiler(
        templateClassName: String,
        templateClasspath: List<File>,
        baseClassLoader: ClassLoader?,
        configuration: CompilerConfiguration,
        projectEnvironment: JavaCoreProjectEnvironment
    ): ReplCompiler = GenericReplCompiler(
        projectEnvironment.parentDisposable,
        makeScriptDefinition(templateClasspath, templateClassName, baseClassLoader),
        configuration,
        configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
    )

    private fun makeScriptDefinition(
        templateClasspath: List<File>, templateClassName: String, baseClassLoader: ClassLoader?
    ): KotlinScriptDefinition = try {
        val classloader = URLClassLoader(templateClasspath.map { it.toURI().toURL() }.toTypedArray(), baseClassLoader)
        val cls = classloader.loadClass(templateClassName)
        KotlinScriptDefinitionFromAnnotatedTemplate(cls.kotlin, emptyMap())
    } catch (ex: ClassNotFoundException) {
        throw IllegalStateException("Cannot find script definition template class $templateClassName", ex)
    } catch (ex: Exception) {
        throw IllegalStateException("Error loading script definition template $templateClassName", ex)
    }
}