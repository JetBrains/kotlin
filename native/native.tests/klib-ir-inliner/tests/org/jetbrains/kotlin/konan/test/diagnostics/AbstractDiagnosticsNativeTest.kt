/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.diagnostics

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.test.Fir2IrNativeResultsConverter
import org.jetbrains.kotlin.konan.test.FirNativeKlibSerializerFacade
import org.jetbrains.kotlin.konan.test.converters.NativeInliningFacade
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.KlibBackendDiagnosticsHandler
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.classicFrontendHandlersStep
import org.jetbrains.kotlin.test.builders.inlinedIrHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.*
import org.jetbrains.kotlin.test.directives.NativeEnvironmentConfigurationDirectives.WITH_PLATFORM_LIBS
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.classic.handlers.DeclarationsDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.configuration.configurationForClassicAndFirTestsAlongside
import org.jetbrains.kotlin.test.configuration.enableLazyResolvePhaseChecking
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.junit.jupiter.api.Assumptions
import java.io.File

abstract class AbstractDiagnosticsNativeTestBase<R : ResultingArtifact.FrontendOutput<R>> : AbstractKotlinCompilerTest() {
    abstract val targetFrontend: FrontendKind<R>
    abstract val frontend: Constructor<FrontendFacade<R>>

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        globalDefaults {
            frontend = targetFrontend
            targetPlatform = NativePlatforms.unspecifiedNativePlatform
            dependencyKind = DependencyKind.Source
        }
        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)
        baseNativeDiagnosticTestConfiguration(frontend)
    }

    override fun runTest(filePath: String) {
        mutePlatformTestIfNecessary(filePath)
        super.runTest(filePath)
    }

    private fun mutePlatformTestIfNecessary(filePath: String) {
        if (HostManager.hostIsMac) return

        if (InTextDirectivesUtils.isDirectiveDefined(File(filePath).readText(), WITH_PLATFORM_LIBS.name))
            Assumptions.abort<Nothing>("Diagnostic tests using platform libs are not supported at non-Mac hosts. Test source: $filePath")
    }
}

abstract class AbstractDiagnosticsNativeTest : AbstractDiagnosticsNativeTestBase<ClassicFrontendOutputArtifact>() {
    override val targetFrontend: FrontendKind<ClassicFrontendOutputArtifact>
        get() = FrontendKinds.ClassicFrontend

    override val frontend: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)

        classicFrontendHandlersStep {
            useHandlers(
                ::DeclarationsDumpHandler,
                ::ClassicDiagnosticsHandler,
            )
        }
        useAdditionalService(::LibraryProvider)
    }
}

abstract class AbstractFirNativeDiagnosticsTestBase(
    private val parser: FirParser
) : AbstractDiagnosticsNativeTestBase<FirOutputArtifact>() {
    override val targetFrontend: FrontendKind<FirOutputArtifact>
        get() = FrontendKinds.FIR

    override val frontend: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)

        configureFirParser(parser)

        baseFirNativeDiagnosticTestConfiguration()
        enableLazyResolvePhaseChecking()

        forTestsMatching("compiler/testData/diagnostics/*") {
            configurationForClassicAndFirTestsAlongside()
        }

        defaultDirectives {
            LanguageSettingsDirectives.LANGUAGE + "+EnableDfaWarningsInK2"
        }
    }
}

abstract class AbstractFirNativeDiagnosticsWithBackendTestBase(parser: FirParser) : AbstractFirNativeDiagnosticsTestBase(parser) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)

        globalDefaults {
            targetBackend = TargetBackend.NATIVE
        }
        defaultDirectives {
            LANGUAGE with "+${LanguageFeature.IrInlinerBeforeKlibSerialization.name}"
        }

        useAdditionalService(::LibraryProvider)

        facadeStep(::Fir2IrNativeResultsConverter)
        facadeStep(::NativeInliningFacade)
        inlinedIrHandlersStep { useHandlers(::IrDiagnosticsHandler) }
        facadeStep(::FirNativeKlibSerializerFacade)

        klibArtifactsHandlersStep {
            useHandlers(::KlibBackendDiagnosticsHandler)
        }
    }
}

abstract class AbstractFirPsiNativeDiagnosticsTest : AbstractFirNativeDiagnosticsTestBase(FirParser.Psi)
abstract class AbstractFirLightTreeNativeDiagnosticsTest : AbstractFirNativeDiagnosticsTestBase(FirParser.LightTree)

abstract class AbstractFirPsiNativeDiagnosticsWithBackendTestBase : AbstractFirNativeDiagnosticsWithBackendTestBase(FirParser.Psi)
abstract class AbstractFirLightTreeNativeDiagnosticsWithBackendTestBase : AbstractFirNativeDiagnosticsWithBackendTestBase(FirParser.LightTree)
