/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.codegen.AbstractAsmLikeInstructionListingTest
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrBlackBoxCodegenTest
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlinx.atomicfu.compiler.extensions.AtomicfuComponentRegistrar
import java.io.File

private val coreLibraryPath = getLibraryJar("kotlinx.atomicfu.AtomicFU")
private val kotlinTestPath = getLibraryJar("kotlin.test.AssertionsKt")
private val javaUtilConcurrentPath = getLibraryJar("java.util.concurrent.atomic.AtomicIntegerFieldUpdater")
private val kotlinJvm = getLibraryJar("kotlin.jvm.JvmField")

open class AbstractAtomicfuJvmIrTest : AbstractIrBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        val librariesPaths = listOf(coreLibraryPath!!, kotlinTestPath!!, javaUtilConcurrentPath!!, kotlinJvm!!)
        builder.configureForKotlinxAtomicfu(librariesPaths)
    }
}

private fun getLibraryJar(classToDetect: String): File? = try {
    PathUtil.getResourcePathForClass(Class.forName(classToDetect))
} catch (e: ClassNotFoundException) {
    null
}

private fun TestConfigurationBuilder.configureForKotlinxAtomicfu(librariesPaths: List<File>) {
    useConfigurators(
        { services ->
            object : EnvironmentConfigurator(services) {
                override fun configureCompilerConfiguration(
                    configuration: CompilerConfiguration,
                    module: TestModule
                ) {
                    configuration.addJvmClasspathRoots(librariesPaths)
                }

                override fun ExtensionStorage.registerCompilerExtensions(module: TestModule, configuration: CompilerConfiguration) {
                    AtomicfuComponentRegistrar.registerExtensions(this)
                }
            }
        })

    useCustomRuntimeClasspathProviders(
        {
            object : RuntimeClasspathProvider(it) {
                override fun runtimeClassPaths(module: TestModule): List<File> = librariesPaths
            }
        }
    )

    defaultDirectives {
        +CodegenTestDirectives.CHECK_BYTECODE_LISTING
    }
}

