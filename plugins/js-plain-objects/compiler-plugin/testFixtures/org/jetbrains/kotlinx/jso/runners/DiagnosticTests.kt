/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.jspo.runners

import org.jetbrains.kotlin.js.test.runners.AbstractJsDiagnosticTestBase
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives
import org.jetbrains.kotlin.test.services.TestPhase

abstract class AbstractFirJsPlainObjectsPluginDiagnosticTest : AbstractJsDiagnosticTestBase(FirParser.Psi) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                TestPhaseDirectives.LATEST_PHASE_IN_PIPELINE with TestPhase.BACKEND
            }

            configureForKotlinxJsPlainObjects()
            disableOptInErrors()
        }
    }
}

private fun TestConfigurationBuilder.disableOptInErrors() {
    defaultDirectives {
        DIAGNOSTICS with listOf("-OPT_IN_USAGE", "-OPT_IN_USAGE_ERROR", "-OPT_IN_TO_INHERITANCE", "-OPT_IN_TO_INHERITANCE_ERROR")
    }
}
