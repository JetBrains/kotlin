/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.test.tests

import kotlin.test.*

private var value = 5
private var tests: MutableSet<String>? = null

class AnnotationsTest {

    @BeforeTest
    fun setup() {
        value *= 2
        assertNull(tests)
        tests = mutableSetOf()
    }

    @AfterTest
    fun teardown() {
        value /= 2
        assertNotNull(tests).let { tests ->
            assertNotEquals(emptySet<String>(), tests)
        }
        tests = null
    }

    private fun logTestRun(name: String) {
        assertNotNull(tests).let { tests ->
            assertEquals(emptySet<String>(), tests)
            tests.add(name)
        }
    }

    @Test
    fun testValue() {
        assertEquals(10, value)
        logTestRun("testValue")
    }

    @Test
    fun testValueAgain() {
        assertEquals(10, value)
        logTestRun("testValueAgain")
    }

    @Ignore
    @Test
    fun testValueWrongIgnored() {
        assertEquals(20, value)
    }

}