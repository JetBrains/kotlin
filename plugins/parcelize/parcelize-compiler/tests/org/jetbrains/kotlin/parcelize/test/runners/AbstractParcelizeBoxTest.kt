/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test.runners

import org.jetbrains.kotlin.parcelize.test.services.ParcelizeDirectives.ENABLE_PARCELIZE
import org.jetbrains.kotlin.parcelize.test.services.ParcelizeEnvironmentConfigurator
import org.jetbrains.kotlin.parcelize.test.services.ParcelizeMainClassProvider
import org.jetbrains.kotlin.parcelize.test.services.ParcelizeRuntimeClasspathProvider
import org.jetbrains.kotlin.parcelize.test.services.ParcelizeUtilSourcesProvider
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureClassicFrontendHandlersStep
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.REQUIRES_SEPARATE_PROCESS
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.runners.codegen.commonConfigurationForTest
import org.jetbrains.kotlin.test.runners.codegen.configureCommonHandlersForBoxTest
import org.jetbrains.kotlin.test.services.jvm.JvmBoxMainClassProvider
import org.jetbrains.kotlin.test.services.service
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider

abstract class AbstractParcelizeBoxTestBase<R : ResultingArtifact.FrontendOutput<R>>(
    val targetFrontend: FrontendKind<R>,
) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    abstract val frontendFacade: Constructor<FrontendFacade<R>>
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<R, IrBackendInput>>

    override fun TestConfigurationBuilder.configuration() {
        defaultDirectives {
            +ENABLE_PARCELIZE
            +REQUIRES_SEPARATE_PROCESS
            +REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
        }

        commonConfigurationForTest(targetFrontend, frontendFacade, frontendToBackendConverter, ::MainFunctionForBlackBoxTestsSourceProvider)

        configureClassicFrontendHandlersStep {
            useHandlers(
                ::ClassicDiagnosticsHandler
            )
        }

        configureFirHandlersStep {
            useHandlers(
                ::FirDiagnosticsHandler
            )
        }

        configureIrHandlersStep {
            useHandlers(::IrTextDumpHandler)
        }

        configureCommonHandlersForBoxTest()

        useCustomRuntimeClasspathProviders(::ParcelizeRuntimeClasspathProvider)

        useConfigurators(::ParcelizeEnvironmentConfigurator)

        useAdditionalSourceProviders(::ParcelizeUtilSourcesProvider)

        useAdditionalServices(service<JvmBoxMainClassProvider>(::ParcelizeMainClassProvider))

        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)

        enableMetaInfoHandler()
    }
}

open class AbstractParcelizeIrBoxTest : AbstractParcelizeBoxTestBase<ClassicFrontendOutputArtifact>(FrontendKinds.ClassicFrontend) {
    override val frontendFacade: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>>
        get() = ::ClassicFrontend2IrConverter
}

abstract class AbstractParcelizeFirBoxTestBase(val parser: FirParser) : AbstractParcelizeBoxTestBase<FirOutputArtifact>(FrontendKinds.FIR) {
    override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrResultsConverter

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +ENABLE_PLUGIN_PHASES
                FirDiagnosticsDirectives.FIR_PARSER with parser
            }
            useAfterAnalysisCheckers(::FirMetaInfoDiffSuppressor)
        }
    }
}

open class AbstractParcelizeFirLightTreeBoxTest : AbstractParcelizeFirBoxTestBase(FirParser.LightTree)
