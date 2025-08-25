/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.diagnostics

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.test.Fir2IrNativeResultsConverter
import org.jetbrains.kotlin.konan.test.NativeKlibSerializerFacade
import org.jetbrains.kotlin.konan.test.converters.NativePreSerializationLoweringFacade
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.KlibBackendDiagnosticsHandler
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.loweredIrHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.*
import org.jetbrains.kotlin.test.directives.NativeEnvironmentConfigurationDirectives.WITH_PLATFORM_LIBS
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

abstract class AbstractDiagnosticsNativeTestBase(
    private val parser: FirParser
) : AbstractKotlinCompilerTest() {

    val targetFrontend: FrontendKind<FirOutputArtifact>
        get() = FrontendKinds.FIR

    val frontend: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        globalDefaults {
            frontend = targetFrontend
            targetPlatform = NativePlatforms.unspecifiedNativePlatform
            dependencyKind = DependencyKind.Source
        }
        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)
        baseNativeDiagnosticTestConfiguration(frontend)

        configureFirParser(parser)

        baseFirNativeDiagnosticTestConfiguration()
        enableLazyResolvePhaseChecking()

        forTestsMatching("compiler/testData/diagnostics/*") {
            configurationForClassicAndFirTestsAlongside()
        }

        defaultDirectives {
            LANGUAGE + "+EnableDfaWarningsInK2"
        }
    }

    override fun runTest(filePath: String) {
        val transformedFilePath = ForTestCompileRuntime.transformTestDataPath(filePath)
        mutePlatformTestIfNecessary(transformedFilePath.path)
        super.runTest(transformedFilePath.path)
    }

    private fun mutePlatformTestIfNecessary(filePath: String) {
        if (HostManager.hostIsMac) return

        if (InTextDirectivesUtils.isDirectiveDefined(File(filePath).readText(), WITH_PLATFORM_LIBS.name))
            Assumptions.abort<Nothing>("Diagnostic tests using platform libs are not supported at non-Mac hosts. Test source: $filePath")
    }
}

abstract class AbstractNativeDiagnosticsWithBackendTestBase(parser: FirParser) : AbstractDiagnosticsNativeTestBase(parser) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)

        globalDefaults {
            targetBackend = TargetBackend.NATIVE
        }

        useAdditionalService(::LibraryProvider)

        facadeStep(::Fir2IrNativeResultsConverter)
        facadeStep(::NativePreSerializationLoweringFacade)
        loweredIrHandlersStep { useHandlers(::IrDiagnosticsHandler) }
        facadeStep(::NativeKlibSerializerFacade)

        klibArtifactsHandlersStep {
            useHandlers(::KlibBackendDiagnosticsHandler)
        }
    }
}

abstract class AbstractNativeDiagnosticsWithBackendWithInlinedFunInKlibTestBase : AbstractNativeDiagnosticsWithBackendTestBase(FirParser.LightTree) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)
        defaultDirectives {
            LANGUAGE with listOf(
                "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
            )
        }
    }
}

abstract class AbstractPsiNativeDiagnosticsTest : AbstractDiagnosticsNativeTestBase(FirParser.Psi)
abstract class AbstractLightTreeNativeDiagnosticsTest : AbstractDiagnosticsNativeTestBase(FirParser.LightTree)

abstract class AbstractPsiNativeDiagnosticsWithBackendTestBase : AbstractNativeDiagnosticsWithBackendTestBase(FirParser.Psi)
abstract class AbstractLightTreeNativeDiagnosticsWithBackendTestBase : AbstractNativeDiagnosticsWithBackendTestBase(FirParser.LightTree)
