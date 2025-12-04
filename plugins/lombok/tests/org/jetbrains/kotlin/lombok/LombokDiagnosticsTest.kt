/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok

import org.jetbrains.kotlin.lombok.k2.KtDiagnosticMessagesLombok
import org.jetbrains.kotlin.lombok.k2.LombokDiagnostics
import org.jetbrains.kotlin.test.utils.verifyDiagnostics
import org.junit.jupiter.api.Test

class LombokDiagnosticsTest {
    @Test
    fun verify() {
        verifyDiagnostics(LombokDiagnostics)
    }
}
