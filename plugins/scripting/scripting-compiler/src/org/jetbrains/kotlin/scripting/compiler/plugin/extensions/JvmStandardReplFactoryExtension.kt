/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.extensions

import com.intellij.core.JavaCoreProjectEnvironment
import org.jetbrains.kotlin.cli.common.extensions.ReplFactoryExtension
import org.jetbrains.kotlin.cli.common.repl.ReplCompiler
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.GenericReplCompiler
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.KotlinScriptDefinitionFromAnnotatedTemplate
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
        configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
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