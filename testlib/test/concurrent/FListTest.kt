package test.concurrent

import std.concurrent.*
import junit.framework.*

class FListTest() : TestCase() {
    fun testEmpty() {
        val empty = FunctionalQueue<Int> ()
        Assert.assertTrue(empty.empty)
    }

    fun testNonEmpty() {
//        val empty = FunctionalList.emptyList<Int> ()
//        assertTrue(!(empty + 10).empty)
    }
}
