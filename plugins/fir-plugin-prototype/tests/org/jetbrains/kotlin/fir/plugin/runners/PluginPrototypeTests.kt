/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin.runners

import org.jetbrains.kotlin.fir.plugin.services.ExtensionRegistrarConfigurator
import org.jetbrains.kotlin.fir.plugin.services.PluginAnnotationsProvider
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_DUMP
import org.jetbrains.kotlin.test.runners.AbstractFirLoadK2CompiledJvmKotlinTest
import org.jetbrains.kotlin.test.runners.AbstractFirPsiDiagnosticTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirLightTreeBlackBoxCodegenTest
import org.jetbrains.kotlin.test.runners.enableLazyResolvePhaseChecking

open class AbstractFirLightTreePluginBlackBoxCodegenTest : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.commonFirWithPluginFrontendConfiguration()
    }
}

abstract class AbstractFirPsiPluginDiagnosticTest : AbstractFirPsiDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.commonFirWithPluginFrontendConfiguration()
    }
}

open class AbstractFirLoadK2CompiledWithPluginJvmKotlinTest : AbstractFirLoadK2CompiledJvmKotlinTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.commonFirWithPluginFrontendConfiguration()
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
}
