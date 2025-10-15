/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir

import org.jetbrains.kotlin.js.test.converters.Fir2IrCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirKlibSerializerCliWebFacade
import org.jetbrains.kotlin.js.test.converters.JsIrPreSerializationLoweringFacade
import org.jetbrains.kotlin.js.test.fir.setUpDefaultDirectivesForJsBoxTest
import org.jetbrains.kotlin.js.test.ir.AbstractJsBlackBoxCodegenTestBase.JsBackendFacades
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_HMPP
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.SEPARATE_KMP_COMPILATION
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.DISABLE_DOUBLE_CHECKING_COMMON_DIAGNOSTICS
import org.jetbrains.kotlin.test.frontend.fir.FirCliMetadataFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirCliMetadataSerializerFacade
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.DelegatingEnvironmentConfiguratorForSeparateKmpCompilation
import org.jetbrains.kotlin.test.services.configuration.JsFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsSecondStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.MetadataEnvironmentConfiguratorForSeparateKmpCompilation
import org.jetbrains.kotlin.test.services.isLeafModuleInMppGraph

abstract class AbstractJsBlackBoxCodegenWithSeparateKmpCompilationTestBase(
    val parser: FirParser,
    private val pathToTestDir: String,
    private val testGroupOutputDirPrefix: String,
) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JS_IR) {

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        setUpDefaultDirectivesForJsBoxTest(parser)
        defaultDirectives {
            +SEPARATE_KMP_COMPILATION
            +DISABLE_DOUBLE_CHECKING_COMMON_DIAGNOSTICS
            +WITH_STDLIB
            DIAGNOSTICS with "-warnings"
        }

        commonServicesConfigurationForJsCodegenTest(
            customConfigurators = listOf(
                ::CommonEnvironmentConfigurator,
                ::MetadataEnvironmentConfiguratorForSeparateKmpCompilation,
                ::JsFirstStageEnvironmentConfiguratorForSeparateKmpCompilation,
                ::JsSecondStageEnvironmentConfiguratorForSeparateKmpCompilation,
            )
        )

        setupFirstStageSteps()
        setupCommonHandlersForJsTest(IGNORE_HMPP)

        commonConfigurationForJsBackendSecondStageTest(
            pathToTestDir,
            testGroupOutputDirPrefix,
            JsBackendFacades.WithRecompilation
        )
        configureJsBoxHandlers()
    }

    private fun TestConfigurationBuilder.setupFirstStageSteps() {
        facadeStep(::FirCliMetadataFrontendFacade)
        facadeStep(::FirCliWebFacade)
        firHandlersStep()
        facadeStep(::FirCliMetadataSerializerFacade)
        facadeStep(::Fir2IrCliWebFacade)
        irHandlersStep()
        facadeStep(::JsIrPreSerializationLoweringFacade)
        loweredIrHandlersStep()
        facadeStep(::FirKlibSerializerCliWebFacade)
        klibArtifactsHandlersStep()
    }
}

class JsFirstStageEnvironmentConfiguratorForSeparateKmpCompilation(
    testServices: TestServices
) : DelegatingEnvironmentConfiguratorForSeparateKmpCompilation(testServices, ::JsFirstStageEnvironmentConfigurator) {
    override fun shouldApply(module: TestModule): Boolean {
        return module.isLeafModuleInMppGraph(testServices)
    }
}

class JsSecondStageEnvironmentConfiguratorForSeparateKmpCompilation(
    testServices: TestServices
) : DelegatingEnvironmentConfiguratorForSeparateKmpCompilation(testServices, ::JsSecondStageEnvironmentConfigurator) {
    override fun shouldApply(module: TestModule): Boolean {
        return module.isLeafModuleInMppGraph(testServices)
    }
}

open class AbstractJsLightTreeBlackBoxCodegenWithSeparateKmpCompilationTest : AbstractJsBlackBoxCodegenWithSeparateKmpCompilationTestBase(
    FirParser.LightTree,
    pathToTestDir = "compiler/testData/codegen/box/multiplatform/k2",
    testGroupOutputDirPrefix = "codegen/irBoxHmpp/lightTree/"
)
