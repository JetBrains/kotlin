/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.registerExtension
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.powerassert.PowerAssertConfigurationDirectives.DISABLE_PLUGIN
import org.jetbrains.kotlin.test.backend.handlers.IrPrettyKotlinDumpHandler
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.irHandlersStep
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.FULL_JDK
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.OPT_IN
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractPhasedJvmDiagnosticPsiTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirLightTreeBlackBoxCodegenTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrBlackBoxCodegenTest
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

// ------------------------ codegen ------------------------

open class AbstractFirLightTreeBlackBoxCodegenTestForPowerAssert : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configurePlugin()
    }
}

abstract class AbstractPowerAssertPluginFirPsiDiagnosticTest : AbstractPhasedJvmDiagnosticPsiTest() {
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

        DIAGNOSTICS + "-POWER_ASSERT_CONSTANT"
        OPT_IN + "kotlinx.powerassert.ExperimentalPowerAssert"
    }
    useDirectives(PowerAssertConfigurationDirectives)

    useConfigurators(::PowerAssertEnvironmentConfigurator)

    useAdditionalSourceProviders(
        ::AdditionalSourceFilesProvider,
    )

    enableRuntime()
    enableJunit()

    irHandlersStep {
        useHandlers(::IrPrettyKotlinDumpHandler)
        useHandlers(::IrDiagnosticsHandler)
    }
}

class PowerAssertEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration,
    ) {
        if (DISABLE_PLUGIN in module.directives) return

        val functions = moduleStructure.allDirectives[PowerAssertConfigurationDirectives.FUNCTION]
            .ifEmpty { listOf("kotlin.assert") }
            .mapTo(mutableSetOf()) { FqName(it) }

        IrGenerationExtension.registerExtension(
            PowerAssertIrGenerationExtension(
                PowerAssertConfiguration(
                    configuration = configuration,
                    functions = functions,
                )
            )
        )
        FirExtensionRegistrar.registerExtension(PowerAssertFirExtensionRegistrar())
    }
}

class AdditionalSourceFilesProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    override val directiveContainers: List<DirectivesContainer> =
        listOf(AdditionalFilesDirectives)

    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure
    ): List<TestFile> {
        return buildList {
            val helpers = "plugins/power-assert/power-assert-compiler/testData/helpers"
            add(ForTestCompileRuntime.transformTestDataPath("$helpers/InfixDispatch.kt").toTestFile())
            add(ForTestCompileRuntime.transformTestDataPath("$helpers/InfixExtension.kt").toTestFile())
            add(ForTestCompileRuntime.transformTestDataPath("$helpers/utils.kt").toTestFile())
        }
    }
}
