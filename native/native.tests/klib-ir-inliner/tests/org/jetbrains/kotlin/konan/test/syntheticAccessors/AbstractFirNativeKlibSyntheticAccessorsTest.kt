/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.syntheticAccessors

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.konan.test.Fir2IrNativeResultsConverter
import org.jetbrains.kotlin.konan.test.FirNativeKlibSerializerFacade
import org.jetbrains.kotlin.konan.test.converters.NativeDeserializerFacade
import org.jetbrains.kotlin.konan.test.converters.NativeInliningFacade
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.SyntheticAccessorsDumpHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.builders.inlinedIrHandlersStep
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives.DUMP_KLIB_SYNTHETIC_ACCESSORS
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.*
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.utils.bind

// Base class for IR dump synthetic accessors test, configured with FIR frontend, in Native-specific way.
open class AbstractFirNativeKlibSyntheticAccessorsTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.NATIVE) {
    val targetFrontend = FrontendKinds.FIR
    val parser = FirParser.LightTree
    val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade
    val frontendToIrConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrNativeResultsConverter
    val irInliningFacade: Constructor<IrInliningFacade<IrBackendInput>>
        get() = ::NativeInliningFacade
    val serializerFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::FirNativeKlibSerializerFacade
    val deserializerFacade: Constructor<DeserializerFacade<BinaryArtifacts.KLib, IrBackendInput>>
        get() = ::NativeDeserializerFacade

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        commonConfigurationForDumpSyntheticAccessorsTest(
            targetFrontend,
            frontendFacade,
            frontendToIrConverter,
            irInliningFacade,
            serializerFacade,
            deserializerFacade,
            KlibBasedCompilerTestDirectives.IGNORE_KLIB_SYNTHETIC_ACCESSORS_CHECKS,
        )
        globalDefaults {
            targetPlatform = NativePlatforms.unspecifiedNativePlatform
        }
        useConfigurators(
            ::NativeEnvironmentConfigurator,
        )
        configureFirParser(parser)
    }
}

// it's a code duplication with same function in AbstractFirJsKlibSyntheticAccessorTest.kt
// consider extracting these functions into one in some common backend module
private fun TestConfigurationBuilder.commonConfigurationForDumpSyntheticAccessorsTest(
    targetFrontend: FrontendKind<FirOutputArtifact>,
    frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>,
    frontendToIrConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>,
    irInliningFacade: Constructor<IrInliningFacade<IrBackendInput>>,
    serializerFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>,
    deserializerFacade: Constructor<DeserializerFacade<BinaryArtifacts.KLib, IrBackendInput>>,
    customIgnoreDirective: ValueDirective<TargetBackend>? = null,
) {
    globalDefaults {
        frontend = targetFrontend
        dependencyKind = DependencyKind.Binary
    }
    defaultDirectives {
        DIAGNOSTICS with listOf("-NOTHING_TO_INLINE", "-ERROR_SUPPRESSION", "-UNCHECKED_CAST")
        LANGUAGE with "+${LanguageFeature.IrInlinerBeforeKlibSerialization.name}"
        +DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
        +ConfigurationDirectives.WITH_STDLIB
        +DUMP_KLIB_SYNTHETIC_ACCESSORS
    }
    useAdditionalService(::LibraryProvider)
    useAdditionalSourceProviders(
        ::CoroutineHelpersSourceFilesProvider,
        ::AdditionalDiagnosticsSourceFilesProvider,
    )
    useAfterAnalysisCheckers(
        ::BlackBoxCodegenSuppressor.bind(customIgnoreDirective),
        ::FirMetaInfoDiffSuppressor,
    )
    facadeStep(frontendFacade)
    firHandlersStep {
        commonFirHandlersForCodegenTest()
        useHandlers(
            ::FirDumpHandler,
            ::FirCfgDumpHandler,
            ::FirCfgConsistencyHandler,
            ::FirResolvedTypesVerifier,
            ::FirDiagnosticsHandler,
        )
    }
    facadeStep(frontendToIrConverter)
    facadeStep(irInliningFacade)

    enableMetaInfoHandler()
    inlinedIrHandlersStep {
        useHandlers(::SyntheticAccessorsDumpHandler)
    }
    facadeStep(serializerFacade)
    facadeStep(deserializerFacade)
}
