package testjc

import kotlin.test.*
import junit.framework.TestCase
import java.util.*

class C()

class JavaClassTest() : TestCase() {
    fun testMe () {
        assertEquals("java.util.ArrayList", java.util.ArrayList<Any>().javaClass.name)
        assertEquals("java.util.ArrayList", ArrayList::class.java.name)
        assertEquals("testjc.C", C::class.java.name)
    }
}