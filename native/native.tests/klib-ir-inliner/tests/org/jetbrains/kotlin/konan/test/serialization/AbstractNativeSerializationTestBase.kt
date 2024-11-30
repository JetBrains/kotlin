/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.serialization

import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.*
import org.jetbrains.kotlin.test.backend.ir.KlibFacades
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.KlibIrInlinerTestDirectives.IGNORE_DESERIALIZED_DUMP_MISMATCH
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.runners.codegen.commonClassicFrontendHandlersForCodegenTest
import org.jetbrains.kotlin.test.runners.ir.klibSteps
import org.jetbrains.kotlin.test.services.GlobalMetadataTestDirectives
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.utils.bind

abstract class AbstractNativeSerializationTestBase<
        R : ResultingArtifact.FrontendOutput<R>,
        I : ResultingArtifact.BackendInput<I>,
        A : ResultingArtifact.Binary<A>,
        >(
    val targetFrontend: FrontendKind<R>,
    targetBackend: TargetBackend,
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    abstract val frontendFacade: Constructor<FrontendFacade<R>>
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<R, I>>
    abstract val irInliningFacade: Constructor<IrInliningFacade<I>>
    abstract val klibFacades: KlibFacades?

    override fun TestConfigurationBuilder.configuration() {
        commonConfigurationForNativeBlackBoxCodegenTest(IGNORE_DESERIALIZED_DUMP_MISMATCH)
    }

    protected fun TestConfigurationBuilder.commonConfigurationForNativeBlackBoxCodegenTest(customIgnoreDirective: ValueDirective<TargetBackend>? = null) {
        commonConfigurationForNativeCodegenTest(
            targetFrontend,
            frontendFacade,
            frontendToBackendConverter,
            irInliningFacade,
            klibFacades,
            customIgnoreDirective
        )

        useDirectives(GlobalMetadataTestDirectives)
        useAdditionalSourceProviders(
            ::CoroutineHelpersSourceFilesProvider,
        )

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
                useHandlers(::AllKlibInterpreterDumpHandler)
            }
        }

        forTestsMatching("compiler/testData/codegen/box/properties/backingField/*") {
            defaultDirectives {
                LanguageSettingsDirectives.LANGUAGE with "+ExplicitBackingFields"
            }
        }
    }
}

@Suppress("reformat")
fun <
        R : ResultingArtifact.FrontendOutput<R>,
        I : ResultingArtifact.BackendInput<I>,
        A : ResultingArtifact.Binary<A>,
        > TestConfigurationBuilder.commonConfigurationForNativeCodegenTest(
    targetFrontend: FrontendKind<R>,
    frontendFacade: Constructor<FrontendFacade<R>>,
    frontendToBackendConverter: Constructor<Frontend2BackendConverter<R, I>>,
    irInliningFacade: Constructor<IrInliningFacade<I>>,
    klibFacades: KlibFacades?,
    customIgnoreDirective: ValueDirective<TargetBackend>? = null,
) {
    globalDefaults {
        frontend = targetFrontend
        targetPlatform = NativePlatforms.unspecifiedNativePlatform
        dependencyKind = DependencyKind.Binary
    }

    defaultDirectives {
        +DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
    }

    useConfigurators(
        ::CommonEnvironmentConfigurator,
        ::NativeEnvironmentConfigurator,
    )

    useAdditionalService(::LibraryProvider)

    useAfterAnalysisCheckers(
        ::BlackBoxCodegenSuppressor.bind(customIgnoreDirective),
    )

    facadeStep(frontendFacade)
    classicFrontendHandlersStep {
        commonClassicFrontendHandlersForCodegenTest()
        useHandlers(::ClassicDiagnosticsHandler)
    }

    firHandlersStep {
        useHandlers(::FirDiagnosticsHandler)
    }

    facadeStep(frontendToBackendConverter)
    irHandlersStep()

    facadeStep(irInliningFacade)

    klibFacades?.let {
        klibSteps(it, false)
        klibArtifactsHandlersStep {
            useHandlers(::KlibBackendDiagnosticsHandler)
            // KT-73171: Add SerializedIrDumpHandler(isAfterDeserialization=false)
        }
        deserializedIrHandlersStep {
            // KT-73171: Add SerializedIrDumpHandler(isAfterDeserialization=true)
        }
    }
}
