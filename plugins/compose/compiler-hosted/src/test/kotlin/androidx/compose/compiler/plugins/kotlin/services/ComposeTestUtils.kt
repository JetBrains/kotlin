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
import org.jetbrains.kotlin.test.services.RuntimeClasspathJsProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.targetPlatform
import java.io.File

private const val JAVA_CLASS_PATH = "java.class.path"
private const val JS_RUNTIME_PATHS = "compose.compiler.test.js.classpath"

private val defaultClassPath by lazy {
    val classPath = System.getProperty(JAVA_CLASS_PATH) ?: error("System property \"$JAVA_CLASS_PATH\" is not found")
    val separator = File.pathSeparator ?: error("File path separator is null")
    classPath.split(separator).map { File(it) }
}

private val jsClassPath by lazy {
    val classPath = System.getProperty(JS_RUNTIME_PATHS) ?: error("System property \"$JS_RUNTIME_PATHS\" is not found")
    val separator = File.pathSeparator ?: error("File path separator is null")
    classPath.split(separator).map { File(it) }
}

class ComposeJvmClasspathConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val platform = module.targetPlatform(testServices)
        when {
            platform.isJvm() -> {
                defaultClassPath.filter { it.absolutePath.contains("androidx.compose") }.forEach {
                    configuration.addJvmClasspathRoot(it)
                }
            }
            else -> error("CodeGen API and compiler tests with Compose compiler plugin are currently supporting only JVM")
        }
    }
}

class ComposeJsClasspathProvider(testServices: TestServices) : RuntimeClasspathJsProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> =
        super.runtimeClassPaths(module) + jsClassPath
}
