/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.utils

import kotlin.test.*

class RepeatTest {
    @Test
    fun testRepeat() {
        var i = 0
        repeat(10) { i++ }
        assertEquals(10, i)
    }
}