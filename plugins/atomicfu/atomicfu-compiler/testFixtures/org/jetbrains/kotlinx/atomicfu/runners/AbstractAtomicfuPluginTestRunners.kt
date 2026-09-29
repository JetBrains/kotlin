/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.runners

import org.jetbrains.kotlin.konan.test.FirCliNativeFacade
import org.jetbrains.kotlin.konan.test.Fir2IrCliNativeFacade
import org.jetbrains.kotlin.konan.test.KlibSerializerNativeCliFacade
import org.jetbrains.kotlin.konan.test.NativePreSerializationLoweringCliFacade
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.createSimpleTestRunSettings
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CustomKlibs
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.handlers.SMAPDumpHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.FirFailingTestSuppressor
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDumpHandler
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractFirPsiDiagnosticTest
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerNativeTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirLightTreeBlackBoxCodegenTest
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.utils.bind
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension

open class AbstractAtomicfuJvmFirLightTreeTest : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureJvmArtifactsHandlersStep { useHandlers(::SMAPDumpHandler) }
        builder.configureForKotlinxAtomicfu()
    }
}

abstract class AbstractAtomicfuNativeFirLightTreeTest : AbstractKotlinCompilerNativeTest() {
    private lateinit var extensionContext: ExtensionContext

    @RegisterExtension
    val extensionContextCaptor = BeforeEachCallback { context ->
        extensionContext = context
    }

    override fun configure(builder: TestConfigurationBuilder) {
        with(builder) {
            globalDefaults {
                frontend = FrontendKinds.FIR
                targetPlatform = NativePlatforms.unspecifiedNativePlatform
                dependencyKind = DependencyKind.Binary
            }
            defaultDirectives {
                +WITH_STDLIB
            }
            useConfigurators(
                ::AtomicfuExtensionRegistrarConfigurator,
                ::CommonEnvironmentConfigurator,
                ::NativeFirstStageEnvironmentConfigurator,
            )
            useCustomRuntimeClasspathProviders(
                ::AtomicfuNativeRuntimeClasspathProvider.bind(
                    extensionContext.createSimpleTestRunSettings().get<CustomKlibs>()
                )
            )
            configureFirParser(FirParser.LightTree)
            facadeStep(::FirCliNativeFacade)
            firHandlersStep {
                useHandlers(
                    ::FirDumpHandler,
                    ::NoFirCompilationErrorsHandler,
                )
            }
            // Produce klibs for regular module dependencies used by later modules' FIR analysis.
            facadeStep(::Fir2IrCliNativeFacade)
            facadeStep(::NativePreSerializationLoweringCliFacade)
            facadeStep(::KlibSerializerNativeCliFacade)
        }
    }
}

abstract class AbstractAtomicfuFirCheckerTest : AbstractFirPsiDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            configureForKotlinxAtomicfu()
            useFailureSuppressors(::FirFailingTestSuppressor)
        }
    }
}
