/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import junit.framework.TestCase

class JoinToReadableStringTest : TestCase() {
    fun test0() {
        assertEquals(
            "",
            listOf<String>().joinToReadableString()
        )
    }

    fun test1() {
        assertEquals(
            "a",
            listOf("a").joinToReadableString()
        )
    }

    fun test2() {
        assertEquals(
            "a and b",
            listOf("a", "b").joinToReadableString()
        )
    }

    fun test3() {
        assertEquals(
            "a, b and c",
            listOf("a", "b", "c").joinToReadableString()
        )
    }

    fun test4() {
        assertEquals(
            "a, b, c and d",
            listOf("a", "b", "c", "d").joinToReadableString()
        )
    }

    fun test5() {
        assertEquals(
            "a, b, c, d and e",
            listOf("a", "b", "c", "d", "e").joinToReadableString()
        )
    }

    fun test6() {
        assertEquals(
            "a, b, c, d, e and 1 more",
            listOf("a", "b", "c", "d", "e", "f").joinToReadableString()
        )
    }
}