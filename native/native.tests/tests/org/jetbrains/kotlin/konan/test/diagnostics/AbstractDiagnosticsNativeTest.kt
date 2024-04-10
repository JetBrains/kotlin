/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.diagnostics

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.test.Fir2IrNativeResultsConverter
import org.jetbrains.kotlin.konan.test.FirNativeKlibBackendFacade
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.KlibBackendDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.classicFrontendHandlersStep
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.*
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_PLATFORM_LIBS
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.classic.handlers.DeclarationsDumpHandler
import org.jetbrains.kotlin.test.frontend.classic.handlers.OldNewInferenceMetaInfoProcessor
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.*
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.runners.configurationForClassicAndFirTestsAlongside
import org.jetbrains.kotlin.test.runners.enableLazyResolvePhaseChecking
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.junit.jupiter.api.Assumptions
import java.io.File

abstract class AbstractDiagnosticsNativeTestBase<R : ResultingArtifact.FrontendOutput<R>> : AbstractKotlinCompilerTest() {
    abstract val targetFrontend: FrontendKind<R>
    abstract val frontend: Constructor<FrontendFacade<R>>

    final override fun TestConfigurationBuilder.configuration() = Unit

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)

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

fun <R : ResultingArtifact.FrontendOutput<R>> TestConfigurationBuilder.baseNativeDiagnosticTestConfiguration(
    frontendFacade: Constructor<FrontendFacade<R>>,
) {
    defaultDirectives {
        +JvmEnvironmentConfigurationDirectives.USE_PSI_CLASS_FILES_READING
        +ConfigurationDirectives.WITH_STDLIB
    }

    enableMetaInfoHandler()

    useConfigurators(
        ::CommonEnvironmentConfigurator,
        ::NativeEnvironmentConfigurator,
    )

    useMetaInfoProcessors(::OldNewInferenceMetaInfoProcessor)
    useAdditionalSourceProviders(
        ::AdditionalDiagnosticsSourceFilesProvider,
        ::CoroutineHelpersSourceFilesProvider,
    )

    facadeStep(frontendFacade)

    forTestsMatching("testData/diagnostics/nativeTests/*") {
        defaultDirectives {
            +LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE
        }
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

        useAdditionalService(::LibraryProvider)

        facadeStep(::Fir2IrNativeResultsConverter)
        facadeStep(::FirNativeKlibBackendFacade)

        klibArtifactsHandlersStep {
            useHandlers(::KlibBackendDiagnosticsHandler)
        }
    }
}

fun TestConfigurationBuilder.baseFirNativeDiagnosticTestConfiguration() {
    firHandlersStep {
        useHandlers(
            ::FirDiagnosticsHandler,
            ::FirDumpHandler,
            ::FirCfgDumpHandler,
            ::FirCfgConsistencyHandler,
            ::FirResolvedTypesVerifier,
            ::FirScopeDumpHandler,
        )
    }
}


abstract class AbstractFirPsiNativeDiagnosticsTest : AbstractFirNativeDiagnosticsTestBase(FirParser.Psi)
abstract class AbstractFirLightTreeNativeDiagnosticsTest : AbstractFirNativeDiagnosticsTestBase(FirParser.LightTree)

abstract class AbstractFirPsiNativeDiagnosticsWithBackendTestBase : AbstractFirNativeDiagnosticsWithBackendTestBase(FirParser.Psi)
abstract class AbstractFirLightTreeNativeDiagnosticsWithBackendTestBase : AbstractFirNativeDiagnosticsWithBackendTestBase(FirParser.LightTree)
