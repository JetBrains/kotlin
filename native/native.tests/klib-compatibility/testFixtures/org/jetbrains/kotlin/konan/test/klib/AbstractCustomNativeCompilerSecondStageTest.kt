/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.konan.test.KlibSerializerNativeCliFacade
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeCoreTest
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives
import org.jetbrains.kotlin.konan.test.configuration.commonConfigurationForNativeFirstStageUpToSerialization
import org.jetbrains.kotlin.konan.test.handlers.NativeBoxRunner
import org.jetbrains.kotlin.konan.test.services.CInteropTestSkipper
import org.jetbrains.kotlin.konan.test.services.DisabledNativeTestSkipper
import org.jetbrains.kotlin.konan.test.services.FileCheckTestTotalSkipper
import org.jetbrains.kotlin.konan.test.services.sourceProviders.NativeLauncherAdditionalSourceProvider
import org.jetbrains.kotlin.test.backend.handlers.KlibAbiDumpHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.builders.nativeArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.DISABLE_FIR_DUMP_HANDLER
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.OPT_IN
import org.jetbrains.kotlin.test.directives.NativeEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.objcinterop.ObjCInteropFacade
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerSecondStageTestSuppressor
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerTestSuppressor
import org.jetbrains.kotlin.test.klib.setupCustomLanguageVersionForKlibCompatibilityTest
import org.jetbrains.kotlin.test.services.TargetBackendTestSkipper
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeSecondStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.UnsupportedFeaturesTestConfigurator
import org.jetbrains.kotlin.utils.bind
import org.junit.jupiter.api.Tag

@Tag("custom-second-stage")
open class AbstractCustomNativeCompilerSecondStageTest : AbstractNativeCoreTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)
        useMetaTestConfigurators(
            ::UnsupportedFeaturesTestConfigurator,
            ::TargetBackendTestSkipper,
            ::DisabledNativeTestSkipper,
            ::CInteropTestSkipper,
            ::FileCheckTestTotalSkipper,
        )
        defaultDirectives {
            +DISABLE_FIR_DUMP_HANDLER
            if (customNativeCompilerSettings.defaultLanguageVersion < LanguageVersion.LATEST_STABLE) {
                // We need to set the custom LV to let `UnsupportedFeaturesTestConfigurator` skip tests with
                // the language features that are not supported in the given custom LV.
                setupCustomLanguageVersionForKlibCompatibilityTest(customNativeCompilerSettings.defaultLanguageVersion)

                LANGUAGE with "+ExportKlibToOlderAbiVersion"
            }
            OPT_IN with listOf(
                "kotlin.native.internal.InternalForKotlinNative",
                "kotlin.native.internal.InternalForKotlinNativeTests",
                "kotlin.experimental.ExperimentalNativeApi"
            )
        }

        val nativeHomeForFirstStage = if (customNativeCompilerSettings.defaultLanguageVersion < LanguageVersion.LATEST_STABLE)
            customNativeCompilerSettings.nativeHome // use home of downloaded distro of previous major version
        else
            null // Use default native home without changing it to a home within downloaded distro of latest major version
        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::NativeFirstStageEnvironmentConfigurator.bind(nativeHomeForFirstStage),
            ::NativeSecondStageEnvironmentConfigurator,
        )
        useAdditionalSourceProviders(
            ::NativeLauncherAdditionalSourceProvider,
        )

        // CInterop-related tests are not that different from regular tests. That's how they work:
        // Modules containing .def files are compiled with ObjCInteropFacade to klib artifact using the current CInterop tool with arguments targeting old ABI.
        // The rest of the 1st stage pipeline will be skipped naturally, since further facades don't accept klibs as input artifacts.
        facadeStep(::ObjCInteropFacade.bind(/*isForwardTest*/true, /*customClassLoader*/null))

        commonConfigurationForNativeFirstStageUpToSerialization()
        facadeStep(::KlibSerializerNativeCliFacade)
        klibArtifactsHandlersStep {
            useHandlers(::KlibAbiDumpHandler)
        }
        configureFirHandlersStep {
            commonFirHandlersForCodegenTest()
        }

        useDirectives(NativeEnvironmentConfigurationDirectives, TestDirectives)
        facadeStep(NativeCompilerSecondStageFacade::NonGrouping.bind(customNativeCompilerSettings))

        nativeArtifactsHandlersStep {
            useHandlers(::NativeBoxRunner)
        }

        useFailureSuppressors(
            // Suppress all tests that failed on the first stage if they are anyway marked as "IGNORE_BACKEND*".
            ::CustomKlibCompilerTestSuppressor,
            // Suppress failed tests having `// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: X.Y.Z`,
            // where `X.Y.Z` matches to `customNativeCompilerSettings.version`
            ::CustomKlibCompilerSecondStageTestSuppressor.bind(customNativeCompilerSettings.defaultLanguageVersion),
        )
        forTestsMatching("compiler/testData/codegen/box/properties/backingField/*") {
            defaultDirectives {
                LANGUAGE with "+ExplicitBackingFields"
            }
        }
    }
}
