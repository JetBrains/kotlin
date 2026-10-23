/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.konan.test.blackbox.AbstractTwoStageNativeCoreTest
import org.jetbrains.kotlin.konan.test.blackbox.NativeGroupingTestIsolator
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives
import org.jetbrains.kotlin.konan.test.handlers.NativeBoxRunnerGroupingStage
import org.jetbrains.kotlin.konan.test.services.CInteropTestSkipper
import org.jetbrains.kotlin.konan.test.services.DisabledNativeTestSkipper
import org.jetbrains.kotlin.konan.test.services.FileCheckTestTotalSkipper
import org.jetbrains.kotlin.konan.test.services.sourceProviders.NativeLauncherAdditionalSourceProvider
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TwoStageTestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.frontend.objcinterop.ObjCInteropFacade
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerFirstStageTestSuppressor
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerTestSuppressor
import org.jetbrains.kotlin.test.klib.setupCustomLanguageVersionForKlibCompatibilityTest
import org.jetbrains.kotlin.test.klib.useReflectionPackageNameHelperIfNeeded
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.ReflectionPackageNameAnnotation
import org.jetbrains.kotlin.test.services.TargetBackendTestSkipper
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeSecondStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.UnsupportedFeaturesTestConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.utils.bind
import org.junit.jupiter.api.Tag

/**
 * KLIB backward-compatibility test:
 * - the non-grouping (first) stage compiles every test into a KLIB with a previously released Kotlin/Native compiler invoked via CLI,
 * - and the grouping (second) stage links batches of such KLIBs into executables with the current compiler and runs them.
 */
@Tag("custom-first-stage")
open class AbstractCustomNativeCompilerFirstStageTest : AbstractTwoStageNativeCoreTest() {
    override fun configure(builder: TwoStageTestConfigurationBuilder): Unit = with(builder) {
        super.configure(builder)
        commonConfiguration {
            globalDefaults {
                frontend = FrontendKinds.FIR
                targetPlatform = NativePlatforms.unspecifiedNativePlatform
                targetBackend = TargetBackend.NATIVE
                dependencyKind = DependencyKind.Binary
            }
            defaultDirectives {
                // We need to set the custom LV to let `UnsupportedFeaturesTestConfigurator` skip tests with
                // the language features that are not supported in the given custom LV.
                setupCustomLanguageVersionForKlibCompatibilityTest(customNativeCompilerSettings.defaultLanguageVersion)

                // K/N does not have minimized stdlib for tests, so need to use the full stdlib
                +WITH_STDLIB
            }
            useMetaTestConfigurators(
                ::UnsupportedFeaturesTestConfigurator,
                ::TargetBackendTestSkipper,
                ::DisabledNativeTestSkipper,
                ::CInteropTestSkipper,
                ::FileCheckTestTotalSkipper,
            )
            useDirectives(TestDirectives)
            // A first-stage stdlib older than 2.5 lacks the annotation, which is then supplied by the helper module below.
            useAdditionalService { ReflectionPackageNameAnnotation }
        }
        nonGroupingStage {
            useConfigurators(
                ::CommonEnvironmentConfigurator,
                ::NativeFirstStageEnvironmentConfigurator.bind(customNativeCompilerSettings.nativeHome),
            )
            useGroupingTestIsolators(::NativeGroupingTestIsolator.bind(CompilationStage.FIRST))
            useAdditionalSourceProviders(
                ::NativeLauncherAdditionalSourceProvider,
                ::CoroutineHelpersSourceFilesProvider,
                ::AdditionalDiagnosticsSourceFilesProvider,
            )
            // The first stage compiles against the custom compiler's own stdlib, which may lack the annotation
            // `BatchingPackageInserter` adds to the files of a grouped test.
            useReflectionPackageNameHelperIfNeeded(customNativeCompilerSettings.defaultLanguageVersion)

            // CInterop-related tests are not that different from regular tests. That's how they work:
            // Modules containing .def files are compiled with ObjCInteropFacade to klib artifact using the old CInterop tool.
            // The rest of the 1st stage pipeline will be skipped naturally, since further facades don't accept klibs as input artifacts.
            facadeStep(::ObjCInteropFacade.bind(/*isForwardTest*/false, customNativeCompilerSettings.compiler.getIsolatedClassLoader()))

            facadeStep(::CustomNativeCompilerFirstStageFacade)

            useFailureSuppressors(
                // Suppress all tests that have not been successfully compiled by the first stage.
                // And the limited number of tests where KLIBs generated by a specific compiler version
                // are known to have some problems that cause crash on the second stage.
                ::CustomKlibCompilerFirstStageTestSuppressor.bind(customNativeCompilerSettings.defaultLanguageVersion),

                // Suppress all tests that failed on the second stage if they are anyway marked as "IGNORE_BACKEND*".
                ::CustomKlibCompilerTestSuppressor,
                ::CustomFirstStageCInteropTestSuppressor,
            )
        }
        groupingStage {
            useConfigurators(::NativeSecondStageEnvironmentConfigurator)
            facadeStep(NativeCompilerSecondStageFacade::Grouping.bind(currentCustomNativeCompilerSettings, /*isCompatibilityTesting*/ true))
            handlersStep(ArtifactKinds.Native, CompilationStage.SECOND) {
                useHandlers(::NativeBoxRunnerGroupingStage)
            }
        }
    }
}
