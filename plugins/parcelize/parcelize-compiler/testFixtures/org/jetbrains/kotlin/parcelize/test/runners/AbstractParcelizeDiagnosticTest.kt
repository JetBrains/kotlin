/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test.runners

import org.jetbrains.kotlin.parcelize.test.services.ParcelizeDirectives.ENABLE_PARCELIZE
import org.jetbrains.kotlin.parcelize.test.services.ParcelizeEnvironmentConfigurator
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.configuration.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.configuration.enableLazyResolvePhaseChecking
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JDK_KIND
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.FirFailingTestSuppressor
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest

abstract class AbstractParcelizeDiagnosticTest : AbstractKotlinCompilerTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        baseFirDiagnosticTestConfiguration()
        enableLazyResolvePhaseChecking()

        defaultDirectives {
            +ENABLE_PARCELIZE
            +ENABLE_PLUGIN_PHASES
            // Robolectric 4.16 (onward) with Android SDK 36 requires JDK 21
            JDK_KIND with TestJdkKind.FULL_JDK_21
        }

        configureFirParser(FirParser.LightTree)

        useConfigurators(::ParcelizeEnvironmentConfigurator)

        useAfterAnalysisCheckers(::FirFailingTestSuppressor)
    }
}
