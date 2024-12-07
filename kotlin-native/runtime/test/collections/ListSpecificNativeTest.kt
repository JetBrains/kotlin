/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.random.Random
import kotlin.test.*

// Native-specific part of stdlib/test/collections/ListSpecificTest.kt
class ListSpecificNativeTest {

    @Test fun factory() {
        val nonConstStr = Random.nextInt().toString()
        val list = listOf(nonConstStr, "b", "c")
        assertEquals(3, list.size)
        assertEquals(nonConstStr, list[0])
        assertEquals("b", list[1])
        assertEquals("c", list[2])
    }
}