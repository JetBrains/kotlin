/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.runners

import org.jetbrains.kotlin.js.test.runners.AbstractPsiJsDiagnosticWithBackendTest
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_DUMP
import org.jetbrains.kotlin.test.runners.AbstractFirPsiDiagnosticTest
import org.jetbrains.kotlinx.serialization.configureForKotlinxSerialization


abstract class AbstractSerializationFirPsiDiagnosticTest : AbstractFirPsiDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureSerializationFirPsiDiagnosticTest()
    }
}

/**
 * For diagnostics that can only be triggered on the JS platform, such as the ones about `dynamic`.
 */
abstract class AbstractSerializationJsDiagnosticTest : AbstractPsiJsDiagnosticWithBackendTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureForKotlinxSerialization(target = TargetBackend.JS_IR)
        builder.disableOptInErrors()
    }
}

fun TestConfigurationBuilder.configureSerializationFirPsiDiagnosticTest() {
    configureForKotlinxSerialization()
    disableOptInErrors()

    forTestsMatching("*/firMembers/*") {
        defaultDirectives {
            +FIR_DUMP
        }
    }
}

internal fun TestConfigurationBuilder.disableOptInErrors() {
    defaultDirectives {
        DIAGNOSTICS with listOf("-OPT_IN_USAGE", "-OPT_IN_USAGE_ERROR", "-OPT_IN_TO_INHERITANCE", "-OPT_IN_TO_INHERITANCE_ERROR")
    }
}
