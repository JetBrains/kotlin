/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test.runners

import org.jetbrains.kotlin.parcelize.test.services.ParcelizeEnvironmentConfigurator
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.AsmLikeInstructionListingDirectives.IR_DIFFERENCE
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrAsmLikeInstructionListingTest

open class AbstractParcelizeIrBytecodeListingTest : AbstractIrAsmLikeInstructionListingTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureParcelizeSpecific()
    }
}

private fun TestConfigurationBuilder.configureParcelizeSpecific() {
    defaultDirectives {
        +IR_DIFFERENCE
    }
    useConfigurators(::ParcelizeEnvironmentConfigurator)
}
