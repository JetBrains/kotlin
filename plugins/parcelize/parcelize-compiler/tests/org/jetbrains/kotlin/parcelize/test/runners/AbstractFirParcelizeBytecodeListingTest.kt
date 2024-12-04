/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test.runners

import org.jetbrains.kotlin.parcelize.test.services.ParcelizeDirectives.ENABLE_PARCELIZE
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirPsiAsmLikeInstructionListingTest
import org.jetbrains.kotlin.parcelize.test.services.ParcelizeEnvironmentConfigurator
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.AsmLikeInstructionListingDirectives.FIR_DIFFERENCE


open class AbstractFirParcelizeBytecodeListingTest : AbstractFirPsiAsmLikeInstructionListingTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureParcelizeSpecific()
    }
}

private fun TestConfigurationBuilder.configureParcelizeSpecific() {
    defaultDirectives {
        +FIR_DIFFERENCE
        +ENABLE_PARCELIZE
    }
    useConfigurators(::ParcelizeEnvironmentConfigurator)
}
