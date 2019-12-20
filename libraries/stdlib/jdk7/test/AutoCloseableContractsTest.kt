/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jdk7.test

import org.junit.Test
import kotlin.test.assertEquals

class AutoCloseableContractsTest {
    @Test
    fun shouldHaveBlockExactlyOnceContract() {
        class TestAutoCloseable : AutoCloseable {
            override fun close() {
            }
        }

        val i: Int
        TestAutoCloseable().use {
            i = 1
        }
        assertEquals(1, i)
    }
}
