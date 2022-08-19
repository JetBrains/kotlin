/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.coroutines.webworkers

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.extensions.IrToJsTransformationExtension
import org.jetbrains.kotlin.js.test.ir.AbstractJsIrTest
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlinx.webworkers.compiler.BetterWebWorkersLoweringExtension
import org.jetbrains.kotlinx.webworkers.compiler.WebWorkersIrToJsExtension
import java.io.File

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

private val jsIrRuntime = System.getProperty("testRuntime.classpath")

open class AbstractBetterWebWorkersJsIrTest : AbstractJsIrTest(
    pathToTestDir = "plugins/better-web-workers/better-web-workers-compiler/testData/box/",
    testGroupOutputDirPrefix = "box/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            useConfigurators(::BetterWebWorkersEnvironmentConfigurator)
            useCustomRuntimeClasspathProviders(::BetterWebWorkersJsRuntimeClasspathProvider)
        }
    }
}

class BetterWebWorkersEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {
        IrGenerationExtension.registerExtension(BetterWebWorkersLoweringExtension())
        IrToJsTransformationExtension.registerExtension(WebWorkersIrToJsExtension())
    }
}

class BetterWebWorkersJsRuntimeClasspathProvider(
    testServices: TestServices
) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        return jsIrRuntime.split(":").map { File(it) }
    }
}