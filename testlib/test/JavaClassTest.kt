package testjc

import stdhack.test.*

class C()

class JavaClassTest() : TestSupport() {
    fun testMe () {
        assertEquals("java.util.ArrayList", java.util.ArrayList<Any>().javaClass.getName())
//        assertEquals("java.util.ArrayList", javaClass<java.util.ArrayList<Any>>().getName())
//        assertEquals("testjc.C", javaClass<C>().getName())
    }
}