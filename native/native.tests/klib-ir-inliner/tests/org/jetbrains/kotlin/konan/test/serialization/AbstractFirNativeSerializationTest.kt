/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.serialization

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.test.Fir2IrNativeResultsConverter
import org.jetbrains.kotlin.konan.test.FirNativeKlibSerializerFacade
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ExternalSourceTransformer
import org.jetbrains.kotlin.konan.test.converters.NativeDeserializerFacade
import org.jetbrains.kotlin.konan.test.converters.NativeInliningFacade
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.IrMangledNameAndSignatureDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.KlibAbiDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.KlibBackendDiagnosticsHandler
import org.jetbrains.kotlin.test.backend.handlers.NativeKlibInterpreterDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.SerializedIrDumpHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.builders.configureKlibArtifactsHandlersStep
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.builders.irHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives.IGNORE_IR_DESERIALIZATION_TEST
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirCfgConsistencyHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirCfgDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirResolvedTypesVerifier
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.runners.codegen.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.utils.bind

// Base class for IR serialization/deserialization test, configured with FIR frontend, in Native-specific way.
open class AbstractFirNativeSerializationTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.NATIVE) {
    val targetFrontend = FrontendKinds.FIR
    val parser = FirParser.LightTree
    val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade
    val frontendToIrConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrNativeResultsConverter
    open val irInliningFacade: Constructor<IrInliningFacade<IrBackendInput>>
        // KT-73624: TODO In a new sub-class AbstractFirNativeSerializationWithInlinedFunInKlibTest, bind NativePreSerializationLoweringPhasesProvider instead
        get() = ::NativeInliningFacade.bind(null)
    val serializerFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::FirNativeKlibSerializerFacade
    val deserializerFacade: Constructor<DeserializerFacade<BinaryArtifacts.KLib, IrBackendInput>>
        get() = ::NativeDeserializerFacade

    final override fun TestConfigurationBuilder.configuration() {
        commonConfigurationForNativeBlackBoxCodegenTest(IGNORE_IR_DESERIALIZATION_TEST)
    }

    protected fun TestConfigurationBuilder.commonConfigurationForNativeBlackBoxCodegenTest(customIgnoreDirective: ValueDirective<TargetBackend>? = null) {
        commonConfigurationForNativeCodegenTest(
            targetFrontend,
            frontendFacade,
            frontendToIrConverter,
            irInliningFacade,
            serializerFacade,
            deserializerFacade,
            customIgnoreDirective
        )
        configureFirParser(parser)

        forTestsNotMatching(
            "compiler/testData/codegen/box/diagnostics/functions/tailRecursion/*" or
                    "compiler/testData/diagnostics/*"
        ) {
            defaultDirectives {
                DIAGNOSTICS with "-warnings"
            }
        }

        enableMetaInfoHandler()

        configureIrHandlersStep {
            useHandlers(::IrMangledNameAndSignatureDumpHandler)
        }

        forTestsMatching("compiler/testData/codegen/box/involvesIrInterpreter/*") {
            enableMetaInfoHandler()
            configureKlibArtifactsHandlersStep {
                useHandlers(::NativeKlibInterpreterDumpHandler)
            }
        }

        forTestsMatching("compiler/testData/codegen/box/properties/backingField/*") {
            defaultDirectives {
                LanguageSettingsDirectives.LANGUAGE with "+ExplicitBackingFields"
            }
        }
    }

    /**
     * Called directly from test class constructor.
     */
    fun register(@TestDataFile testDataFilePath: String, sourceTransformer: ExternalSourceTransformer) {}
}

@Suppress("reformat")
fun TestConfigurationBuilder.commonConfigurationForNativeCodegenTest(
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
        targetPlatform = NativePlatforms.unspecifiedNativePlatform
        dependencyKind = DependencyKind.Binary
    }

    defaultDirectives {
        +DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
        +ConfigurationDirectives.WITH_STDLIB
    }

    useConfigurators(
        ::NativeEnvironmentConfigurator,
    )

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

    irHandlersStep { useHandlers({ SerializedIrDumpHandler(it, isAfterDeserialization = false) }) }

    facadeStep(serializerFacade)
    klibArtifactsHandlersStep {
        useHandlers(::KlibAbiDumpHandler)
    }
    facadeStep(deserializerFacade)
    klibArtifactsHandlersStep {
        useHandlers(::KlibBackendDiagnosticsHandler)
    }
    // TODO: KT-73171 uncomment the following line after fixing discrepancies between IR in before-serialization and after-deserialization state.
    // Or as soon those tests with discrepancies are muted.
    // irHandlersStep { useHandlers({ SerializedIrDumpHandler(it, isAfterDeserialization = true) }) }
}
