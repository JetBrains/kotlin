/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.runners

import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedBlackBoxTest
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlinx.serialization.configureForKotlinxSerialization

abstract class AbstractLLReversedSerializationBlackBoxTest : AbstractLLReversedBlackBoxTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureForKotlinxSerialization()
    }
}
