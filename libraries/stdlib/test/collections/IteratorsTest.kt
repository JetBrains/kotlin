package test.collections

import kotlin.test.*

class IteratorsTest {
    @Test fun iterationOverIterator() {
        val c = listOf(0, 1, 2, 3, 4, 5)
        var s = ""
        for (i in c.iterator()) {
            s = s + i.toString()
        }
        assertEquals("012345", s)
    }
}
