/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.konan.test.KlibSerializerNativeCliFacade
import org.jetbrains.kotlin.konan.test.configuration.commonConfigurationForNativeCodegenTest
import org.jetbrains.kotlin.konan.test.configuration.setupStepsForNativeFirstStageUpToSerialization
import org.jetbrains.kotlin.konan.test.handlers.FileCheckHandler
import org.jetbrains.kotlin.konan.test.handlers.NativeBoxRunnerGroupingPhase
import org.jetbrains.kotlin.konan.test.klib.NativeCompilerSecondStageFacade
import org.jetbrains.kotlin.konan.test.klib.currentCustomNativeCompilerSettings
import org.jetbrains.kotlin.konan.test.services.CInteropTestSkipper
import org.jetbrains.kotlin.konan.test.services.DisabledNativeTestSkipper
import org.jetbrains.kotlin.konan.test.services.FileCheckTestSkipper
import org.jetbrains.kotlin.konan.test.services.sourceProviders.NativeLauncherAdditionalSourceProvider
import org.jetbrains.kotlin.konan.test.suppressors.NativeTestsSuppressor
import org.jetbrains.kotlin.test.backend.handlers.NoIrCompilationErrorsHandler
import org.jetbrains.kotlin.test.builders.TwoPhaseTestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.builders.configureLoweredIrHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.OPT_IN
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.frontend.objcinterop.ObjCInteropFacade
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeSecondStageEnvironmentConfigurator
import org.jetbrains.kotlin.utils.bind

abstract class AbstractNativeCodegenBoxCoreTest : AbstractTwoPhaseNativeCoreTest() {
    override fun configure(builder: TwoPhaseTestConfigurationBuilder): Unit = with(builder) {
        super.configure(builder)
        commonConfiguration {
            defaultDirectives {
                LANGUAGE with listOf(
                    "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                    "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
                )
                OPT_IN with listOf(
                    "kotlin.native.internal.InternalForKotlinNative",
                    "kotlin.native.internal.InternalForKotlinNativeTests",
                    "kotlin.experimental.ExperimentalNativeApi"
                )
            }

            commonConfigurationForNativeCodegenTest()

            useMetaTestConfigurators(::DisabledNativeTestSkipper, ::CInteropTestSkipper, ::FileCheckTestSkipper)
            useFailureSuppressors(
                ::FirMetaInfoDiffSuppressor,
                ::NativeTestsSuppressor,
            )
        }

        nonGroupingPhase {
            useConfigurators(
                ::CommonEnvironmentConfigurator,
                ::NativeFirstStageEnvironmentConfigurator,
            )

            useGroupingTestIsolators(::NativeGroupingTestIsolator)
            facadeStep(::ObjCInteropFacade)

            // Because of package escaping various dumps for grouping mode would be different from
            // the regular one, so we don't want all the frontend handlers to be set up, only some specific ones.
            setupStepsForNativeFirstStageUpToSerialization(
                includeBasicFirHandlers = true,
                includeDumpFirHandlers = false
            )

            configureIrHandlersStep {
                useHandlers(::NoIrCompilationErrorsHandler)
            }

            configureLoweredIrHandlersStep {
                useHandlers(::NoIrCompilationErrorsHandler)
            }

            facadeStep(::KlibSerializerNativeCliFacade)
            klibArtifactsHandlersStep()

            useAdditionalSourceProviders(
                ::NativeLauncherAdditionalSourceProvider,
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
        }

        groupingPhase {
            useConfigurators(::NativeSecondStageEnvironmentConfigurator)

            facadeStep(NativeCompilerSecondStageFacade::Grouping.bind(currentCustomNativeCompilerSettings))
            handlersStep(ArtifactKinds.Native, CompilationStage.SECOND) {
                useHandlers(::NativeBoxRunnerGroupingPhase, ::FileCheckHandler)
            }
        }
    }
}
