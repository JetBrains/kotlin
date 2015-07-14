package test.utils

import kotlin.*
import kotlin.test.*
import org.junit.Test as test

private class PartiallyImplementedClass {
    public val prop: String get() = TODO

    internal fun method1() = TODO as String
    public fun method2(): Int = TODO

    public fun method3(switch: Boolean, value: String): String {
        if (!switch)
            TODO
        else {
            if (value.length() < 3)
                throw TODO
            else
                return value
        }
    }
}

class TODOTest {
    test fun usage() {
        val inst = PartiallyImplementedClass()

        assertTrue(fails { inst.prop } is UnsupportedOperationException)
        assertTrue(fails { inst.method1() } is UnsupportedOperationException)
        assertTrue(fails { inst.method2() } is UnsupportedOperationException)
        assertTrue(fails { inst.method3(false, "test") } is UnsupportedOperationException)
        assertTrue(fails { inst.method3(true, "t") } is UnsupportedOperationException)
        assertEquals("test", inst.method3(true, "test"))
    }
}
