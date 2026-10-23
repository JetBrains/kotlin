/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.konan.test.KlibSerializerNativeCliFacade
import org.jetbrains.kotlin.konan.test.blackbox.AbstractTwoStageNativeCoreTest
import org.jetbrains.kotlin.konan.test.blackbox.NativeGroupingTestIsolator
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives
import org.jetbrains.kotlin.konan.test.configuration.commonConfigurationForNativeCodegenTest
import org.jetbrains.kotlin.konan.test.configuration.setupStepsForNativeFirstStageUpToSerialization
import org.jetbrains.kotlin.konan.test.handlers.NativeBoxRunnerGroupingStage
import org.jetbrains.kotlin.konan.test.services.CInteropTestSkipper
import org.jetbrains.kotlin.konan.test.services.DisabledNativeTestSkipper
import org.jetbrains.kotlin.konan.test.services.FileCheckTestTotalSkipper
import org.jetbrains.kotlin.konan.test.services.sourceProviders.NativeLauncherAdditionalSourceProvider
import org.jetbrains.kotlin.test.backend.handlers.KlibAbiDumpHandler
import org.jetbrains.kotlin.test.builders.TwoStageTestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.OPT_IN
import org.jetbrains.kotlin.test.directives.NativeEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.objcinterop.ObjCInteropFacade
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerSecondStageTestSuppressor
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerTestSuppressor
import org.jetbrains.kotlin.test.klib.KlibOnlyFirstStageStateReleaser
import org.jetbrains.kotlin.test.klib.REFLECTION_PACKAGE_NAME_SINCE
import org.jetbrains.kotlin.test.klib.isolateReflectionPackageNameDependentTestsIfNeeded
import org.jetbrains.kotlin.test.klib.setupCustomLVForKlibForwardCompatibilityTest
import org.jetbrains.kotlin.test.klib.useReflectionPackageNameAnnotationIfSupported
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TargetBackendTestSkipper
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeSecondStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.UnsupportedFeaturesTestConfigurator
import org.jetbrains.kotlin.utils.bind
import org.junit.jupiter.api.Tag

/**
 * KLIB forward-compatibility test:
 * - the non-grouping (first) stage compiles every test into a KLIB with the current compiler,
 * - and the grouping (second) stage links batches of such KLIBs into executables with a previously released Kotlin/Native compiler
 *   invoked via CLI, and runs them.
 */
@Tag("custom-second-stage")
open class AbstractCustomNativeCompilerSecondStageTest : AbstractTwoStageNativeCoreTest() {
    override fun configure(builder: TwoStageTestConfigurationBuilder): Unit = with(builder) {
        super.configure(builder)
        val customLanguageVersion = customNativeCompilerSettings.defaultLanguageVersion
        commonConfiguration {
            commonConfigurationForNativeCodegenTest()
            useMetaTestConfigurators(
                ::UnsupportedFeaturesTestConfigurator,
                ::TargetBackendTestSkipper,
                ::DisabledNativeTestSkipper,
                ::CInteropTestSkipper,
                ::FileCheckTestTotalSkipper,
            )
            defaultDirectives {
                setupCustomLVForKlibForwardCompatibilityTest(customLanguageVersion)

                OPT_IN with listOf(
                    "kotlin.native.internal.InternalForKotlinNative",
                    "kotlin.experimental.ExperimentalNativeApi"
                )
            }
            useDirectives(NativeEnvironmentConfigurationDirectives, TestDirectives)
            // The annotation `BatchingPackageInserter` adds to the files of a grouped test must be known to the first-stage stdlib,
            // which is the custom compiler's own one for an older LV, and to the second-stage linker, which is the custom compiler.
            useReflectionPackageNameAnnotationIfSupported(customLanguageVersion)

            forTestsMatching("compiler/testData/codegen/box/properties/backingField/*") {
                defaultDirectives {
                    LANGUAGE with "+ExplicitBackingFields"
                }
            }
        }
        nonGroupingStage {
            val nativeHomeForFirstStage = if (customLanguageVersion < LanguageVersion.LATEST_STABLE)
                customNativeCompilerSettings.nativeHome // use home of downloaded distro of previous major version
            else
                null // Use default native home without changing it to a home within downloaded distro of latest major version
            useConfigurators(
                ::CommonEnvironmentConfigurator,
                ::NativeFirstStageEnvironmentConfigurator.bind(nativeHomeForFirstStage),
            )
            useGroupingTestIsolators(::NativeGroupingTestIsolator.bind(CompilationStage.SECOND))
            isolateReflectionPackageNameDependentTestsIfNeeded(customLanguageVersion)
            // Besides the reflective package names, Kotlin/Native renders qualified names in `KClass`/`KType.toString()`.
            if (customLanguageVersion < REFLECTION_PACKAGE_NAME_SINCE) useGroupingTestIsolators(::NativeQualifiedNameRenderingTestIsolator)
            useAdditionalSourceProviders(
                ::NativeLauncherAdditionalSourceProvider,
            )

            // Because of package escaping various dumps for grouping mode would be different from
            // the regular one, so we don't want all the frontend handlers to be set up, only some specific ones.
            setupStepsForNativeFirstStageUpToSerialization(
                includeBasicFirHandlers = true,
                includeDumpFirHandlers = false,
            )
            facadeStep(::KlibSerializerNativeCliFacade)
            klibArtifactsHandlersStep {
                // Tests with KLIB ABI dumps are not renamed: `NativeGroupingTestIsolator` isolates them.
                useHandlers(::KlibAbiDumpHandler)
            }
            // Modules containing .def files are compiled with ObjCInteropFacade to klib artifact using the current CInterop tool
            // with arguments targeting old ABI. Both `KlibSerializerNativeCliFacade` and `ObjCInteropFacade` produce Klib artifact,
            // so the latter one must run last, for its output to be used for compilation of other modules.
            facadeStep(::ObjCInteropFacade.bind(/*isForwardTest*/true, /*customClassLoader*/null))
            useAfterAnalysisCheckers(::KlibOnlyFirstStageStateReleaser)

            useFailureSuppressors(
                // Suppress all tests that failed on the first stage if they are anyway marked as "IGNORE_BACKEND*".
                ::CustomKlibCompilerTestSuppressor,
                // Suppress failed tests having `// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: X.Y.Z`,
                // where `X.Y.Z` matches to `customNativeCompilerSettings.version`
                ::CustomKlibCompilerSecondStageTestSuppressor.bind(customLanguageVersion),
            )
        }
        groupingStage {
            useConfigurators(::NativeSecondStageEnvironmentConfigurator)
            facadeStep(NativeCompilerSecondStageFacade::Grouping.bind(customNativeCompilerSettings, /*isCompatibilityTesting*/ true))
            handlersStep(ArtifactKinds.Native, CompilationStage.SECOND) {
                useHandlers(::NativeBoxRunnerGroupingStage)
            }
        }
    }
}
