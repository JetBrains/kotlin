/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.runners

import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.createSimpleTestRunSettings
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CustomKlibs
import org.jetbrains.kotlin.konan.test.irText.AbstractLightTreeNativeIrTextTest
import org.jetbrains.kotlin.test.backend.handlers.SMAPDumpHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.NativeEnvironmentConfigurationDirectives.WITH_PLATFORM_LIBS
import org.jetbrains.kotlin.test.frontend.fir.FirFailingTestSuppressor
import org.jetbrains.kotlin.test.runners.AbstractFirPsiDiagnosticTest
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

abstract class AbstractAtomicfuFirCheckerTest : AbstractFirPsiDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            configureForKotlinxAtomicfu()
            useFailureSuppressors(::FirFailingTestSuppressor)
        }
    }
}

open class AbstractAtomicfuNativeIrTextTest : AbstractLightTreeNativeIrTextTest() {
    private lateinit var extensionContext: ExtensionContext

    @RegisterExtension
    val extensionContextCaptor = BeforeEachCallback { context ->
        extensionContext = context
    }

    override val irDumpOnly: Boolean
        get() = true

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            useConfigurators(
                ::AtomicfuExtensionRegistrarConfigurator,
                ::CommonEnvironmentConfigurator,
                ::NativeFirstStageEnvironmentConfigurator,
            )
            defaultDirectives {
                +WITH_PLATFORM_LIBS
            }

            useCustomRuntimeClasspathProviders(
                ::AtomicfuNativeRuntimeClasspathProvider.bind(
                    extensionContext.createSimpleTestRunSettings().get<CustomKlibs>()
                )
            )
        }
    }
}
