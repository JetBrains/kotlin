/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import org.jetbrains.kotlin.test.MuteableTestRule
import org.junit.Rule
import kotlin.test.*
import java.util.*

class IteratorsJVMTest {
    @get:Rule val muteableTestRule = MuteableTestRule()

    @Test fun testEnumeration() {
        val v = Vector<Int>()
        for (i in 1..5)
            v.add(i)

        var sum = 0
        for (k in v.elements())
            sum += k

        assertEquals(15, sum)
    }
}
