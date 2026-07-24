/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.headerMode

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.cli.common.arguments.allowTestsOnlyLanguageFeatures
import org.jetbrains.kotlin.konan.test.KlibSerializerNativeCliFacade
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeCoreTest
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives
import org.jetbrains.kotlin.konan.test.configuration.commonConfigurationForNativeFirstStageUpToSerialization
import org.jetbrains.kotlin.konan.test.services.CInteropTestSkipper
import org.jetbrains.kotlin.konan.test.services.DisabledNativeTestSkipper
import org.jetbrains.kotlin.konan.test.services.sourceProviders.NativeLauncherAdditionalSourceProvider
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_HEADER_MODE
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.HEADER_MODE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.OPT_IN
import org.jetbrains.kotlin.test.directives.NativeEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.objcinterop.ObjCInteropFacade
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeFirstStageEnvironmentConfigurator

abstract class AbstractNativeCodegenBoxCoreHeaderModeTest : AbstractNativeCoreTest() {
    override fun runTest(@TestDataFile filePath: String) {
        allowTestsOnlyLanguageFeatures()
        super.runTest(filePath)
    }

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)
        useAdditionalService(::LibraryProvider)
        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::NativeFirstStageEnvironmentConfigurator,
        )
        useDirectives(NativeEnvironmentConfigurationDirectives, TestDirectives, LanguageSettingsDirectives)
        useMetaTestConfigurators(::DisabledNativeTestSkipper, ::CInteropTestSkipper)

        // 1st stage (sources -> klibs)
        useAdditionalSourceProviders(
            ::NativeLauncherAdditionalSourceProvider,
        )
        // Modules containing .def files are compiled with ObjCInteropFacade to klib using the CInterop tool.
        // The rest of the 1st stage pipeline will be skipped naturally, since 1st stage facades don't accept klibs as input artifact.
        // The pipeline for the 2nd stage will be skipped, since cinterop klibs do not represent a main module in tests
        facadeStep(::ObjCInteropFacade)

        commonConfigurationForNativeFirstStageUpToSerialization(
            customIgnoreDirective = IGNORE_HEADER_MODE,
            includeDumpFirHandlers = false
        )
        facadeStep(::KlibSerializerNativeCliFacade)
        klibArtifactsHandlersStep()

        defaultDirectives {
            OPT_IN with listOf(
                "kotlin.native.internal.InternalForKotlinNative",
                "kotlin.experimental.ExperimentalNativeApi"
            )
            +HEADER_MODE
            DIAGNOSTICS with "-warnings"
        }
    }
}
