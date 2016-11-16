package test.numbers

import kotlin.test.*
import org.junit.Test

// TODO: Run these tests during compiler test only (JVM & JS)
class BitwiseOperationsTest {
    @Test fun orForInt() {
        assertEquals(3, 2 or 1)
    }

    @Test fun andForInt() {
        assertEquals(0, 1 and 0)
    }

    @Test fun xorForInt() {
        assertEquals(1, 2 xor 3)
    }

    @Test fun shlForInt() {
        assertEquals(4, 1 shl 2)
    }

    @Test fun shrForInt() {
        assertEquals(1, 2 shr 1)
    }

    @Test fun ushrForInt() {
        assertEquals(2147483647, -1 ushr 1)
    }

    @Test fun invForInt() {
        assertEquals(0, (-1).inv())
    }
}