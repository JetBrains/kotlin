/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test.runners

import org.jetbrains.kotlin.parcelize.test.services.ParcelizeDirectives.ENABLE_PARCELIZE
import org.jetbrains.kotlin.parcelize.test.services.ParcelizeEnvironmentConfigurator
import org.jetbrains.kotlin.parcelize.test.services.ParcelizeMainClassProvider
import org.jetbrains.kotlin.parcelize.test.services.ParcelizeRuntimeClasspathProvider
import org.jetbrains.kotlin.parcelize.test.services.ParcelizeUtilSourcesProvider
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.configuration.configureCommonHandlersForBoxTest
import org.jetbrains.kotlin.test.configuration.setupJvmPipelineSteps
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.REQUIRES_SEPARATE_PROCESS
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JDK_KIND
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.jvm.JvmBoxMainClassProvider
import org.jetbrains.kotlin.test.services.service
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider

abstract class AbstractParcelizeBoxTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        defaultDirectives {
            +ENABLE_PARCELIZE
            +REQUIRES_SEPARATE_PROCESS
            +REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
            // Robolectric 4.16 (onward) with Android SDK 36 requires JDK 21
            JDK_KIND with TestJdkKind.FULL_JDK_21
            +ENABLE_PLUGIN_PHASES
        }

        setupJvmPipelineSteps(FirParser.LightTree)

        configureFirHandlersStep {
            useHandlers(
                ::FirDiagnosticsHandler
            )
        }

        configureIrHandlersStep {
            useHandlers(::IrTextDumpHandler)
        }

        configureCommonHandlersForBoxTest()

        useCustomRuntimeClasspathProviders(::ParcelizeRuntimeClasspathProvider)
        useConfigurators(::ParcelizeEnvironmentConfigurator)
        useAdditionalSourceProviders(::ParcelizeUtilSourcesProvider, ::MainFunctionForBlackBoxTestsSourceProvider)
        useAdditionalServices(service<JvmBoxMainClassProvider>(::ParcelizeMainClassProvider))
        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor, ::FirMetaInfoDiffSuppressor)
        enableMetaInfoHandler()
    }
}
