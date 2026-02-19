/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.utils

import kotlin.test.*

private class TopLevel

private fun Any.toStringWithoutHashCode(): String {
    val string = toString()
    assertEquals(1, string.count { it == '@' }, "Invalid toString() value: $string")
    return string.substringBefore('@')
}

class AnyTest {
    @Test
    fun testToString() {
        assertEquals("test.utils.TopLevel", TopLevel().toStringWithoutHashCode())

        class Local1
        assertEquals("test.utils.AnyTest\$testToString\$Local1", Local1().toStringWithoutHashCode())

        assertEquals("test.utils.AnyTest\$testToString\$1", object {}.toStringWithoutHashCode())

        fun localFun() {
            class Local2
            assertEquals("test.utils.AnyTest\$testToString\$localFun\$Local2", Local2().toStringWithoutHashCode())

            assertEquals("test.utils.AnyTest\$testToString\$localFun\$1", object {}.toStringWithoutHashCode())
        }
        localFun()
    }
}