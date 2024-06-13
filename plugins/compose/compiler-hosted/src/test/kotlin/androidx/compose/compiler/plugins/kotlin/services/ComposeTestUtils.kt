/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin.services

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

private const val JAVA_CLASS_PATH = "java.class.path"

private val defaultClassPath by lazy {
    val classPath = System.getProperty(JAVA_CLASS_PATH) ?: error("System property \"$JAVA_CLASS_PATH\" is not found")
    val separator = File.pathSeparator ?: error("File path separator is null")
    classPath.split(separator).map { File(it) }
}

class ComposePluginAnnotationsProvider(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val platform = module.targetPlatform
        when {
            platform.isJvm() -> {
                defaultClassPath.firstOrNull { it.absolutePath.contains("compose") && it.absolutePath.contains("runtime") }?.let {
                    configuration.addJvmClasspathRoot(it)
                }
            }
            else -> error("CodeGen API and compiler tests with Compose compiler plugin are currently supporting only JVM")
        }
    }
}