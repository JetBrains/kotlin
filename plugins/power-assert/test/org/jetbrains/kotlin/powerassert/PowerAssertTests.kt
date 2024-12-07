/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.backend.handlers.IrPrettyKotlinDumpHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.irHandlersStep
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.FULL_JDK
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirLightTreeBlackBoxCodegenTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrBlackBoxCodegenTest
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

// ------------------------ codegen ------------------------

open class AbstractIrBlackBoxCodegenTestForPowerAssert : AbstractIrBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configurePlugin()
    }
}

open class AbstractFirLightTreeBlackBoxCodegenTestForPowerAssert : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configurePlugin()
    }
}

// ------------------------ configuration ------------------------

fun TestConfigurationBuilder.configurePlugin() {
    defaultDirectives {
        +FULL_JDK
        +WITH_STDLIB
    }
    useDirectives(PowerAssertConfigurationDirectives)

    useConfigurators(::PowerAssertEnvironmentConfigurator)

    useAdditionalSourceProviders(
        ::AdditionalSourceFilesProvider,
    )

    enableJunit()

    irHandlersStep {
        useHandlers(::IrPrettyKotlinDumpHandler)
    }
}

class PowerAssertEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration,
    ) {
        val functions = moduleStructure.allDirectives[PowerAssertConfigurationDirectives.FUNCTION]
            .ifEmpty { listOf("kotlin.assert") }
            .mapTo(mutableSetOf()) { FqName(it) }

        IrGenerationExtension.registerExtension(PowerAssertIrGenerationExtension(PowerAssertConfiguration(configuration, functions)))
    }
}

class AdditionalSourceFilesProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    override val directiveContainers: List<DirectivesContainer> =
        listOf(AdditionalFilesDirectives)

    override fun produceAdditionalFiles(globalDirectives: RegisteredDirectives, module: TestModule): List<TestFile> {
        return buildList {
            add(File("plugins/power-assert/testData/helpers/InfixDispatch.kt").toTestFile())
            add(File("plugins/power-assert/testData/helpers/InfixExtension.kt").toTestFile())
            add(File("plugins/power-assert/testData/helpers/utils.kt").toTestFile())
        }
    }
}
