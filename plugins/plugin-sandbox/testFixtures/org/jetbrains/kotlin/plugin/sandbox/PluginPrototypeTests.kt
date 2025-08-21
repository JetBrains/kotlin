/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox

import org.jetbrains.kotlin.js.test.fir.AbstractFirJsTest
import org.jetbrains.kotlin.js.test.fir.AbstractFirLoadK2CompiledJsKotlinTest
import org.jetbrains.kotlin.js.test.ir.AbstractJsBlackBoxCodegenWithSeparateKmpCompilationTestBase
import org.jetbrains.kotlin.kotlinp.jvm.test.CompareMetadataHandler
import org.jetbrains.kotlin.plugin.sandbox.PluginSandboxDirectives.DONT_LOAD_IN_SYNTHETIC_MODULES
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.IrPrettyKotlinDumpHandler
import org.jetbrains.kotlin.test.backend.ir.BackendCliJvmFacade
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.commonConfigurationForJvmTest
import org.jetbrains.kotlin.test.configuration.enableLazyResolvePhaseChecking
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.DISABLE_FIR_DUMP_HANDLER
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_DUMP
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirFailingTestSuppressor
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractFirLoadK2CompiledJvmKotlinTest
import org.jetbrains.kotlin.test.runners.AbstractFirPsiDiagnosticTest
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirLightTreeBlackBoxCodegenTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractJvmBlackBoxCodegenWithSeparateKmpCompilationTestBase

open class AbstractFirJvmLightTreePluginBlackBoxCodegenTest : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.commonFirWithPluginFrontendConfiguration()
    }
}

open class AbstractFirJvmLightTreePluginBlackBoxCodegenWithSeparateKmpCompilationTest :
    AbstractJvmBlackBoxCodegenWithSeparateKmpCompilationTestBase(parser = FirParser.LightTree) {

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.commonFirWithPluginFrontendConfiguration()
    }
}

open class AbstractFirJsLightTreePluginBlackBoxCodegenTest : AbstractFirJsTest(
    pathToTestDir = "plugins/plugin-sandbox/testData/box",
    testGroupOutputDirPrefix = "firPluginSandboxBox/",
    parser = FirParser.LightTree
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.commonFirWithPluginFrontendConfiguration(dumpFir = false)
        builder.defaultDirectives {
            +DISABLE_FIR_DUMP_HANDLER
        }
    }
}

open class AbstractFirJsLightTreePluginBlackBoxCodegenWithSeparateKmpCompilationTest :
    AbstractJsBlackBoxCodegenWithSeparateKmpCompilationTestBase(
        pathToTestDir = "plugins/plugin-sandbox/testData/box",
        testGroupOutputDirPrefix = "firPluginSandboxBox/",
        parser = FirParser.LightTree
    ) {

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.commonFirWithPluginFrontendConfiguration(dumpFir = false)
        builder.defaultDirectives {
            +DISABLE_FIR_DUMP_HANDLER
        }
    }
}

abstract class AbstractFirPsiPluginDiagnosticTest : AbstractFirPsiDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            commonFirWithPluginFrontendConfiguration()
            useAfterAnalysisCheckers(::FirFailingTestSuppressor)
        }
    }
}

open class AbstractFirLoadK2CompiledWithPluginJvmKotlinTest : AbstractFirLoadK2CompiledJvmKotlinTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +DONT_LOAD_IN_SYNTHETIC_MODULES
            }
            commonFirWithPluginFrontendConfiguration()
            configureIrHandlersStep {
                useHandlers(::IrPrettyKotlinDumpHandler)
            }
        }
    }
}

open class AbstractFirLoadK2CompiledWithPluginJsKotlinTest : AbstractFirLoadK2CompiledJsKotlinTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +DONT_LOAD_IN_SYNTHETIC_MODULES
            }
            commonFirWithPluginFrontendConfiguration()
        }
    }
}

open class AbstractFirMetadataPluginSandboxTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    override fun configure(builder: TestConfigurationBuilder) {
        with(builder) {
            commonConfigurationForJvmTest(FrontendKinds.FIR, ::FirCliJvmFacade, ::Fir2IrCliJvmFacade, ::BackendCliJvmFacade)
            configureFirHandlersStep {
                useHandlers(::FirDiagnosticsHandler)
            }
            enableMetaInfoHandler()
            configureFirParser(FirParser.LightTree)
            commonFirWithPluginFrontendConfiguration(dumpFir = false)
            configureJvmArtifactsHandlersStep {
                useHandlers({ CompareMetadataHandler(it, extension = ".metadata.txt") })
            }
        }
    }
}

fun TestConfigurationBuilder.commonFirWithPluginFrontendConfiguration(dumpFir: Boolean = true) {
    enableLazyResolvePhaseChecking()

    defaultDirectives {
        +ENABLE_PLUGIN_PHASES
        if (dumpFir) {
            +FIR_DUMP
        }
    }

    useConfigurators(
        ::PluginAnnotationsProvider,
        ::ExtensionRegistrarConfigurator
    )

    useCustomRuntimeClasspathProviders(
        ::PluginRuntimeAnnotationsProvider
    )

    useAfterAnalysisCheckers(
        ::FirFailingTestSuppressor,
    )
}
