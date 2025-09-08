/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.runners

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.js.test.fir.AbstractFirJsTest
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathJsProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlinx.atomicfu.compiler.extensions.AtomicfuLoweringExtension
import java.io.File

private val atomicfuJsCompileDependency = System.getProperty("atomicfuJs.classpath")
private val atomicfuJsIrRuntime = System.getProperty("atomicfuJsIrRuntimeForTests.classpath")

open class AbstractAtomicfuJsTest(
    testGroupOutputDirPrefix: String = "box/atomicfu"
) : AbstractFirJsTest(
    pathToTestDir = "plugins/atomicfu/atomicfu-compiler/testData/box/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            useConfigurators(::AtomicfuEnvironmentConfigurator)
            useCustomRuntimeClasspathProviders(::AtomicfuJsRuntimeClasspathProvider)
        }
    }
}

open class AbstractAtomicfuJsWithInlinedFunInKlibTest : AbstractAtomicfuJsTest(
    testGroupOutputDirPrefix = "box/atomicfuWithInlinedFunInKlib"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                LANGUAGE with listOf(
                    "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                    "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
                )
            }
        }
    }
}

private class AtomicfuEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun legacyRegisterCompilerExtensions(project: Project, module: TestModule, configuration: CompilerConfiguration) {
        IrGenerationExtension.registerExtension(project, AtomicfuLoweringExtension())
    }
}

private class AtomicfuJsRuntimeClasspathProvider(
    testServices: TestServices
) : RuntimeClasspathJsProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        return super.runtimeClassPaths(module) + listOf(atomicfuJsCompileDependency, atomicfuJsIrRuntime).map { File(it) }
    }
}
