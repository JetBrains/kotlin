/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.test.tests

import kotlin.test.*

private var value = 5

class AnnotationsTest {

    @BeforeTest fun setup() {
        value *= 2
    }

    @AfterTest fun teardown() {
        value /= 2
    }

    @Test fun testValue() {
        assertEquals(10, value)
    }

    @Test fun testValueAgain() {
        assertEquals(10, value)
    }

    @Ignore @Test fun testValueWrong() {
        assertEquals(20, value)
    }

}