/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.runners

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.AsmLikeInstructionListingDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_DUMP
import org.jetbrains.kotlin.test.runners.AbstractFirPsiDiagnosticTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirLightTreeAsmLikeInstructionListingTest
import org.jetbrains.kotlin.test.runners.configurationForClassicAndFirTestsAlongside
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.rhizomedb.configureForRhizomedb

//abstract class AbstractSerializationPluginDiagnosticTest : AbstractDiagnosticTest() {
//    override fun configure(builder: TestConfigurationBuilder) {
//        super.configure(builder)
//        with(builder) {
//            configureForKotlinxSerialization()
//            disableOptInErrors()
//        }
//    }
//}

abstract class AbstractRhizomedbFirPsiDiagnosticTest : AbstractFirPsiDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            configureForRhizomedb()
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

open class AbstractRhizomedbFirLightTreeAsmLikeInstructionsListingTest : AbstractFirLightTreeAsmLikeInstructionListingTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useAdditionalService(::LibraryProvider)
        builder.defaultDirectives {
            +AsmLikeInstructionListingDirectives.FIR_DIFFERENCE
        }
        builder.configureForRhizomedb()
    }
}

private fun TestConfigurationBuilder.disableOptInErrors() {
    defaultDirectives {
        DIAGNOSTICS with listOf("-OPT_IN_USAGE", "-OPT_IN_USAGE_ERROR")
    }
}
