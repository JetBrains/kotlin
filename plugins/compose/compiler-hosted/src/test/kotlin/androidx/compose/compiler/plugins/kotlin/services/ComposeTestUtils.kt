/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin.services

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.disableIrCheckers
import org.jetbrains.kotlin.ir.validation.checkers.declaration.IrClassSuperTypesChecker
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathJsProvider
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.targetPlatform
import java.io.File

private const val JAVA_CLASS_PATH = "java.class.path"
private const val JS_RUNTIME_PATHS = "compose.compiler.test.js.classpath"
private const val WASM_RUNTIME_PATHS = "compose.compiler.test.wasm.classpath"

private val defaultClassPath by lazy {
    val classPath = System.getProperty(JAVA_CLASS_PATH) ?: error("System property \"$JAVA_CLASS_PATH\" is not found")
    val separator = File.pathSeparator ?: error("File path separator is null")
    classPath.split(separator).map { File(it) }
}

private val composeLibraries by lazy {
    defaultClassPath.filter { it.absolutePath.contains("androidx.compose") }
}

private fun classPathProperty(name: String): List<File> {
    val classPath = System.getProperty(name) ?: error("System property \"$name\" is not found")
    val separator = File.pathSeparator ?: error("File path separator is null")
    return classPath.split(separator).map { File(it) }
}

private val jsClassPath by lazy { classPathProperty(JS_RUNTIME_PATHS) }

private val wasmClassPath by lazy { classPathProperty(WASM_RUNTIME_PATHS) }

class ComposeJvmClasspathConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val platform = module.targetPlatform(testServices)
        when {
            platform.isJvm() -> {
                composeLibraries.forEach {
                    configuration.addJvmClasspathRoot(it)
                }
            }
            else -> error("CodeGen API and compiler tests with Compose compiler plugin are currently supporting only JVM")
        }
    }
}

class ComposeJvmClasspathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        return composeLibraries
    }
}

class ComposeJsClasspathProvider(testServices: TestServices) : RuntimeClasspathJsProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> =
        super.runtimeClassPaths(module) + jsClassPath
}

/**
 * The Compose runtime klib used in tests is compiled with Kotlin 2.3.20, whose Compose plugin generated `ComposableSingletons`
 * objects without supertypes (fixed in 2.5.0 by KT-87962). No test directive disables this check.
 */
class ComposeWasmLibrariesIrValidationConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    // The libraries are deserialized and validated when linking
    override val compilationStage: CompilationStage
        get() = CompilationStage.SECOND

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        configuration.disableIrCheckers += IrClassSuperTypesChecker::class.java.simpleName
    }
}

class ComposeWasmClasspathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> = wasmClassPath
}
