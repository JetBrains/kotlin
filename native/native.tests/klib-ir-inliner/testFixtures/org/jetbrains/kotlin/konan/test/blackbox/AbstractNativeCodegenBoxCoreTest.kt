/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.cli.common.arguments.allowTestsOnlyLanguageFeatures
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.konan.test.Fir2IrNativeResultsConverter
import org.jetbrains.kotlin.konan.test.NativeKlibSerializerFacade
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives
import org.jetbrains.kotlin.konan.test.configuration.commonConfigurationForNativeFirstStageUpToSerialization
import org.jetbrains.kotlin.konan.test.converters.NativePreSerializationLoweringFacade
import org.jetbrains.kotlin.konan.test.handlers.NativeRunnerHandler
import org.jetbrains.kotlin.konan.test.klib.NativeCompilerSecondStageFacade
import org.jetbrains.kotlin.konan.test.klib.currentCustomNativeCompilerSettings
import org.jetbrains.kotlin.konan.test.services.DisabledNativeTestSkipper
import org.jetbrains.kotlin.konan.test.services.sourceProviders.NativeLauncherAdditionalSourceProvider
import org.jetbrains.kotlin.konan.test.suppressors.NativeTestsSuppressor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.backend.handlers.FirInterpreterDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.NativeKlibInterpreterDumpHandler
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.OPT_IN
import org.jetbrains.kotlin.test.directives.NativeEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.frontend.objcinterop.ObjCInteropFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.utils.bind

abstract class AbstractNativeCodegenBoxCoreTest : AbstractNativeCoreTest() {
    override fun runTest(@TestDataFile filePath: String) {
        allowTestsOnlyLanguageFeatures()
        super.runTest(filePath)
    }

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)
        configureFirParser(FirParser.LightTree)
        useAdditionalService(::LibraryProvider)
        useConfigurators(::NativeEnvironmentConfigurator)
        useDirectives(NativeEnvironmentConfigurationDirectives, TestDirectives, LanguageSettingsDirectives)
        useMetaTestConfigurators(::DisabledNativeTestSkipper)
        enableMetaInfoHandler()
        useAfterAnalysisCheckers(
            ::FirMetaInfoDiffSuppressor,
            ::NativeTestsSuppressor,
        )

        // 1st stage (sources -> klibs)
        useAdditionalSourceProviders(
            ::NativeLauncherAdditionalSourceProvider,
        )
        // Modules containing .def files are compiled with ObjCInteropFacade to klib using the CInterop tool.
        // The rest of the 1st stage pipeline will be skipped naturally, since 1st stage facades don't accept klibs as input artifact.
        // The pipeline for the 2nd stage will be skipped, since cinterop klibs do not represent a main module in tests
        facadeStep(::ObjCInteropFacade)

        commonConfigurationForNativeFirstStageUpToSerialization(
            FrontendKinds.FIR,
            ::FirFrontendFacade,
            ::Fir2IrNativeResultsConverter,
            ::NativePreSerializationLoweringFacade,
        )
        facadeStep(::NativeKlibSerializerFacade)
        klibArtifactsHandlersStep()

        // 2nd stage (klibs -> executable)
        facadeStep(::NativeCompilerSecondStageFacade.bind(currentCustomNativeCompilerSettings))
        nativeArtifactsHandlersStep {
            useHandlers(::NativeRunnerHandler)
        }

        defaultDirectives {
            LANGUAGE with listOf(
                "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
            )
            OPT_IN with listOf(
                "kotlin.native.internal.InternalForKotlinNative",
                "kotlin.experimental.ExperimentalNativeApi"
            )
        }
        forTestsNotMatching(
            "compiler/testData/codegen/box/diagnostics/functions/tailRecursion/*" or
                    "compiler/testData/diagnostics/*"
        ) {
            defaultDirectives {
                DIAGNOSTICS with "-warnings"
            }
        }

        forTestsMatching("compiler/testData/codegen/box/involvesIrInterpreter/*") {
            configureFirHandlersStep {
                useHandlers(::FirInterpreterDumpHandler)
            }
            configureKlibArtifactsHandlersStep {
                useHandlers(::NativeKlibInterpreterDumpHandler)
            }
        }
    }
}
