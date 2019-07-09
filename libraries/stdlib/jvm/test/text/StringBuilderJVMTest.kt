/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import kotlin.test.*

class StringBuilderJVMTest() {

    @Test fun stringBuildWithInitialCapacity() {
        val s = buildString(123) {
            assertEquals(123, capacity())
        }
        assertEquals("", s)
    }

    @Test fun getAndSetChar() {
        val sb = StringBuilder("abc")
        sb[1] = 'z'

        assertEquals("azc", sb.toString())
        assertEquals('c', sb[2])
    }
}
