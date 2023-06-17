/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:OptIn(FreezingIsDeprecated::class, kotlin.experimental.ExperimentalNativeApi::class)
package codegen.objectDeclaration.isFrozen

import kotlin.test.*
import kotlin.native.concurrent.*

object X {
    var value: Int = 0
}

@Test fun runTest() {
    if (Platform.memoryModel == MemoryModel.STRICT) {
        assertTrue(X.isFrozen)
        assertFailsWith<InvalidMutabilityException> {
            X.value = 42
        }
        assertEquals(0, X.value)
    } else {
        assertFalse(X.isFrozen)
        X.value = 42
        assertEquals(42, X.value)
    }
}
