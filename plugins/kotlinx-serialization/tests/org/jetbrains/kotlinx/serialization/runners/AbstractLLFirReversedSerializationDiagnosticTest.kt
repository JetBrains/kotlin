/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.runners

import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLFirPreresolvedReversedDiagnosticCompilerTestDataTest
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlinx.serialization.runners.configureSerializationFirPsiDiagnosticTest

abstract class AbstractLLFirReversedSerializationDiagnosticTest : AbstractLLFirPreresolvedReversedDiagnosticCompilerTestDataTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.configureSerializationFirPsiDiagnosticTest()
    }
}
