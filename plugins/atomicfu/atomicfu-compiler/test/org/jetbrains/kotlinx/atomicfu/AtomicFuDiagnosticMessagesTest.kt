/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu

import org.jetbrains.kotlin.test.utils.verifyDiagnostics
import org.jetbrains.kotlinx.atomicfu.compiler.diagnostic.AtomicfuErrors
import org.junit.jupiter.api.Test

class AtomicFuDiagnosticMessagesTest {
    @Test
    fun verifyDiagnosticMessages() {
        verifyDiagnostics(AtomicfuErrors)
    }
}
