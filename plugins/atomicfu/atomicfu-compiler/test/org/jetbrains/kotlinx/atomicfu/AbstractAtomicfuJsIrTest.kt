/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.js.test.ir.AbstractJsIrTest
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathJsProvider
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlinx.atomicfu.compiler.extensions.AtomicfuLoweringExtension
import java.io.File

private val atomicfuJsCompileDependency = System.getProperty("atomicfuJs.classpath")
private val atomicfuJsIrRuntime = System.getProperty("atomicfuJsIrRuntimeForTests.classpath")

open class AbstractAtomicfuJsIrTest : AbstractJsIrTest(
    pathToTestDir = "plugins/atomicfu/atomicfu-compiler/testData/box/",
    testGroupOutputDirPrefix = "box/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            useConfigurators(::AtomicfuEnvironmentConfigurator)
            useCustomRuntimeClasspathProviders(::AtomicfuJsRuntimeClasspathProvider)
        }
    }
}

class AtomicfuEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun legacyRegisterCompilerExtensions(project: Project, module: TestModule, configuration: CompilerConfiguration) {
        IrGenerationExtension.registerExtension(project, AtomicfuLoweringExtension())
    }
}

class AtomicfuJsRuntimeClasspathProvider(
    testServices: TestServices
) : RuntimeClasspathJsProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        return super.runtimeClassPaths(module) + listOf(atomicfuJsCompileDependency, atomicfuJsIrRuntime).map { File(it) }
    }
}
