/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.configuration

import org.jetbrains.kotlin.konan.test.Fir2IrCliNativeFacade
import org.jetbrains.kotlin.konan.test.FirCliNativeFacade
import org.jetbrains.kotlin.konan.test.NativePreSerializationLoweringCliFacade
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilderBase
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.configuration.commonCodegenConfiguration
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.frontend.fir.handlers.*
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.utils.bind


fun TestConfigurationBuilder.commonConfigurationForNativeFirstStageUpToSerialization(
    customIgnoreDirective: ValueDirective<TargetBackend>? = null,
) {
    commonConfigurationForNativeCodegenTest(customIgnoreDirective = customIgnoreDirective)
    setupStepsForNativeFirstStageUpToSerialization()
}

/**
 * Sets up directives and services which are applicable for both stages of
 * any native codegen test.
 */
fun TestConfigurationBuilderBase<*, *>.commonConfigurationForNativeCodegenTest(
    firParser: FirParser = FirParser.LightTree,
    customIgnoreDirective: ValueDirective<TargetBackend>? = null,
) {
    commonCodegenConfiguration()

    globalDefaults {
        frontend = FrontendKinds.FIR
        targetPlatform = NativePlatforms.unspecifiedNativePlatform
        targetBackend = TargetBackend.NATIVE
        dependencyKind = DependencyKind.Binary
    }

    defaultDirectives {
        +DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
        +ConfigurationDirectives.WITH_STDLIB
    }
    configureFirParser(firParser)

    useAdditionalService(::LibraryProvider)
    useAdditionalSourceProviders(
        ::CoroutineHelpersSourceFilesProvider,
        ::AdditionalDiagnosticsSourceFilesProvider,
    )
    useAfterAnalysisCheckers(
        ::BlackBoxCodegenSuppressor.bind(customIgnoreDirective, null),
    )
}

/**
 * Sets up all first-stage steps from frontend till pre-serialization lowerings (included).
 * Also, sets up corresponding handlers steps for each facade step.
 */
fun TestConfigurationBuilder.setupStepsForNativeFirstStageUpToSerialization(includeFirHandlers: Boolean = true) {
    facadeStep(::FirCliNativeFacade)
    firHandlersStep {
        commonFirHandlersForCodegenTest()
        if (includeFirHandlers) {
            useHandlers(
                ::FirDumpHandler,
                ::FirCfgDumpHandler,
                ::FirCfgConsistencyHandler,
                ::FirResolvedTypesVerifier,
                ::FirDiagnosticsHandler,
            )
        }
    }

    facadeStep(::Fir2IrCliNativeFacade)
// TODO(KT-85460): uncomment and fix failing tests
//    irHandlersStep()

    facadeStep(::NativePreSerializationLoweringCliFacade)
// TODO(KT-85460): uncomment and fix failing tests
//    loweredIrHandlersStep()
}
