/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.runners

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlinx.atomicfu.compiler.extensions.AtomicfuFirExtensionRegistrar
import org.jetbrains.kotlinx.atomicfu.compiler.extensions.AtomicfuLoweringExtension
import java.io.File

internal fun TestConfigurationBuilder.configureForKotlinxAtomicfu() {
    useConfigurators(
        ::AtomicfuExtensionRegistrarConfigurator
    )

    useCustomRuntimeClasspathProviders(
        {
            object : RuntimeClasspathProvider(it) {
                override fun runtimeClassPaths(module: TestModule): List<File> = getLibrariesPaths()
            }
        }
    )

    defaultDirectives {
        +CodegenTestDirectives.CHECK_BYTECODE_LISTING
    }
}

class AtomicfuExtensionRegistrarConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        configuration.addJvmClasspathRoots(getLibrariesPaths())
    }

    override fun ExtensionStorage.registerCompilerExtensions(module: TestModule, configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(AtomicfuFirExtensionRegistrar())
        IrGenerationExtension.registerExtension(AtomicfuLoweringExtension())
    }
}

private fun getLibraryJar(classToDetect: String): File? = try {
    PathUtil.getResourcePathForClass(Class.forName(classToDetect))
} catch (e: ClassNotFoundException) {
    null
}

private fun getLibrariesPaths(): List<File> {
    val coreLibraryPath = getLibraryJar("kotlinx.atomicfu.AtomicFU") ?: error("kotlinx.atomicfu library is not found")
    val kotlinTestPath = getLibraryJar("kotlin.test.AssertionsKt") ?: error("kotlin.test is not found")
    val kotlinJvm = getLibraryJar("kotlin.jvm.JvmField") ?: error("kotlin-stdlib is not found")
    return listOf(coreLibraryPath, kotlinTestPath, kotlinJvm)
}

