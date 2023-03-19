/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.runners

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_DUMP
import org.jetbrains.kotlin.test.runners.AbstractDiagnosticTest
import org.jetbrains.kotlin.test.runners.AbstractFirPsiDiagnosticTest
import org.jetbrains.kotlin.test.runners.configurationForClassicAndFirTestsAlongside
import org.jetbrains.kotlinx.serialization.configureForKotlinxSerialization

abstract class AbstractSerializationPluginDiagnosticTest : AbstractDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            configureForKotlinxSerialization()
            disableOptInErrors()
        }
    }
}

abstract class AbstractSerializationFirPsiDiagnosticTest : AbstractFirPsiDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            configureForKotlinxSerialization()
            disableOptInErrors()

            forTestsMatching("*/diagnostics/*") {
                configurationForClassicAndFirTestsAlongside()
            }

            forTestsMatching("*/firMembers/*") {
                defaultDirectives {
                    +FIR_DUMP
                }
            }
        }
    }
}

private fun TestConfigurationBuilder.disableOptInErrors() {
    defaultDirectives {
        DIAGNOSTICS with listOf("-OPT_IN_USAGE", "-OPT_IN_USAGE_ERROR")
    }
}
