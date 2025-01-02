/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox

import org.jetbrains.kotlin.js.test.fir.AbstractFirLoadK2CompiledJsKotlinTest
import org.jetbrains.kotlin.plugin.sandbox.PluginSandboxDirectives.DONT_LOAD_IN_SYNTHETIC_MODULES
import org.jetbrains.kotlin.test.backend.handlers.IrPrettyKotlinDumpHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_DUMP
import org.jetbrains.kotlin.test.frontend.fir.FirFailingTestSuppressor
import org.jetbrains.kotlin.test.runners.AbstractFirLoadK2CompiledJvmKotlinTest
import org.jetbrains.kotlin.test.runners.AbstractFirPsiDiagnosticTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirLightTreeBlackBoxCodegenTest
import org.jetbrains.kotlin.test.configuration.enableLazyResolvePhaseChecking

open class AbstractFirLightTreePluginBlackBoxCodegenTest : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.commonFirWithPluginFrontendConfiguration()
    }
}

abstract class AbstractFirPsiPluginDiagnosticTest : AbstractFirPsiDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            commonFirWithPluginFrontendConfiguration()
            useAfterAnalysisCheckers(::FirFailingTestSuppressor)
        }
    }
}

open class AbstractFirLoadK2CompiledWithPluginJvmKotlinTest : AbstractFirLoadK2CompiledJvmKotlinTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +DONT_LOAD_IN_SYNTHETIC_MODULES
            }
            commonFirWithPluginFrontendConfiguration()
            configureIrHandlersStep {
                useHandlers(::IrPrettyKotlinDumpHandler)
            }
        }
    }
}

open class AbstractFirLoadK2CompiledWithPluginJsKotlinTest : AbstractFirLoadK2CompiledJsKotlinTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +DONT_LOAD_IN_SYNTHETIC_MODULES
            }
            commonFirWithPluginFrontendConfiguration()
        }
    }
}

fun TestConfigurationBuilder.commonFirWithPluginFrontendConfiguration() {
    enableLazyResolvePhaseChecking()

    defaultDirectives {
        +ENABLE_PLUGIN_PHASES
        +FIR_DUMP
    }

    useConfigurators(
        ::PluginAnnotationsProvider,
        ::ExtensionRegistrarConfigurator
    )

    useCustomRuntimeClasspathProviders(
        ::PluginRuntimeAnnotationsProvider
    )

    useAfterAnalysisCheckers(
        ::FirFailingTestSuppressor,
    )
}

