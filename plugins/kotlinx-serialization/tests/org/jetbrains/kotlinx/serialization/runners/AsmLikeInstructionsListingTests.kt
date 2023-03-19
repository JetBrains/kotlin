/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.runners

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.AsmLikeInstructionListingDirectives
import org.jetbrains.kotlin.test.runners.codegen.AbstractAsmLikeInstructionListingTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirLightTreeAsmLikeInstructionListingTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrAsmLikeInstructionListingTest
import org.jetbrains.kotlinx.serialization.configureForKotlinxSerialization

open class AbstractSerializationAsmLikeInstructionsListingTest : AbstractAsmLikeInstructionListingTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enableDifference()
        builder.configureForKotlinxSerialization()
    }
}

open class AbstractSerializationIrAsmLikeInstructionsListingTest : AbstractIrAsmLikeInstructionListingTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enableDifference()
        builder.configureForKotlinxSerialization()
    }
}

open class AbstractSerializationFirLightTreeAsmLikeInstructionsListingTest : AbstractFirLightTreeAsmLikeInstructionListingTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            +AsmLikeInstructionListingDirectives.FIR_DIFFERENCE
        }
        builder.configureForKotlinxSerialization()
    }
}

private fun TestConfigurationBuilder.enableDifference() {
    defaultDirectives {
        +AsmLikeInstructionListingDirectives.IR_DIFFERENCE
    }
}
