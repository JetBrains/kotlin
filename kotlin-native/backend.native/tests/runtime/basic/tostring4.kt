/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.basic.tostring4

import kotlin.test.*

class TopLevel

@Test fun runTest() {
    assertEquals("runtime.basic.tostring4.TopLevel", TopLevel().toStringWithoutHashCode())

    class Local1
    assertEquals("runtime.basic.tostring4.runTest\$Local1", Local1().toStringWithoutHashCode())

    assertEquals("runtime.basic.tostring4.runTest\$1", object {}.toStringWithoutHashCode())

    fun localFun() {
        class Local2
        assertEquals("runtime.basic.tostring4.runTest\$localFun\$Local2", Local2().toStringWithoutHashCode())

        assertEquals("runtime.basic.tostring4.runTest\$localFun\$1", object {}.toStringWithoutHashCode())
    }
    localFun()
}

private fun Any.toStringWithoutHashCode(): String {
    val string = toString()
    assertEquals(1, string.count { it == '@' }, "Invalid toString() value: $string")
    return string.substringBefore('@')
}
