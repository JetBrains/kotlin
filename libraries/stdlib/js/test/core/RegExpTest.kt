/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.js

import kotlin.js.*

import kotlin.test.*

class RegExpTest {

    @Test fun regExpToString() {
        val pattern = "q(\\d+)d"
        val re = RegExp(pattern, "i")
        assertEquals("/$pattern/i", re.toString())
    }

    @Test fun regExpProperties() {
        val re1 = RegExp("[a-z]", "img")
        assertTrue(re1.global)
        assertTrue(re1.ignoreCase)
        assertTrue(re1.multiline)
        val re2 = RegExp("\\d")
        assertFalse(re2.global)
        assertFalse(re2.ignoreCase)
        assertFalse(re2.multiline)
    }

    @Test fun regExpTest() {
        val pattern = "q(\\d+)d"
        val re = RegExp(pattern, "i")

        assertTrue(re.test("test q12D string"))
        assertFalse(re.test("sample"))

        assertFalse(RegExp("\\w").test("?"))
    }


    @Test fun regExpExec() {
        val string = "R2D2 beats A5D5 "
        var re = RegExp("""(\w\d)(\w\d)""", "g")
        val m1 = re.exec(string)!!
        assertEquals(listOf("R2D2", "R2", "D2"), m1.asArray().asList())
        assertEquals(0, m1.index)
        assertEquals(4, re.lastIndex)

        val m2 = re.exec(string)!!
        assertEquals(listOf("A5D5", "A5", "D5"), m2.asArray().asList())
        assertEquals(string.indexOf(m2[0]!!), m2.index)

        val noMatch = re.exec(string)
        assertEquals(null, noMatch)
        assertEquals(0, re.lastIndex)
    }
}
