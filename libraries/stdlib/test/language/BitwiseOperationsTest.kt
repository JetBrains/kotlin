package test

import kotlin.test.*
import org.junit.Test as test

class BitwiseOperationsTest {
    test fun orForInt() {
        assertEquals(3, 2 or 1)
    }

    test fun andForInt() {
        assertEquals(0, 1 and 0)
    }

    test fun xorForInt() {
        assertEquals(1, 2 xor 3)
    }

    test fun shlForInt() {
        assertEquals(4, 1 shl 2)
    }

    test fun shrForInt() {
        assertEquals(1, 2 shr 1)
    }

    test fun ushrForInt() {
        assertEquals(2147483647, -1 ushr 1)
    }

    test fun invForInt() {
        assertEquals(0, (-1).inv())
    }
}