package app

import kotlin.test.Test
import kotlin.test.assertEquals

class LinuxX64Text {
    @Test
    fun test1() {
        assertEquals(cinterop.dummy.foo_linux(), 42)
    }
}