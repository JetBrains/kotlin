/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.collections.AbstractMutableCollection

import kotlin.test.*

class TestCollection(): AbstractMutableCollection<Int>() {
    companion object {
        const val SIZE = 7
    }

    private val array = IntArray(SIZE)
    private var len = 0

    override val size: Int
        get() = len

    override fun add(element: Int): Boolean {
        if (len >= SIZE) return false
        array[len++] = element
        return true
    }

    override fun iterator(): MutableIterator<Int> = object: MutableIterator<Int> {
        var nextIndex = 0

        override fun hasNext() = nextIndex < len
        override fun next() = array[nextIndex++]

        override fun remove() {
            if (nextIndex == 0) throw IllegalStateException()
            for (i in nextIndex..len - 1) {
                array[i - 1] = array[i]
            }
            len--
            nextIndex--
        }
    }

    override fun clear() {
        len = 0
    }

}

fun assertEquals(a: TestCollection, b: List<Int>) {
    if (a.size != b.size) {
        throw AssertionError()
    }
    val aIt = a.iterator()
    val bIt = b.iterator()
    while (aIt.hasNext()) {
        if (aIt.next() != bIt.next()) throw AssertionError("TestCollection contains wrong elements. Expected: $b, actual: $a.")
    }
}

@Test fun runTest() {
    val c = TestCollection()
    if (!c.addAll(listOf(1, 2, 3, 2, 4, 5, 4))) throw AssertionError("addAll is false when it must be true.")
    if (c.addAll(listOf(1, 2)) != false) throw AssertionError("addAll is true when it must be false.")
    c.removeAll(listOf(1, 2))
    assertEquals(c, listOf(3, 4, 5, 4))
    c.retainAll(listOf(4, 5))
    assertEquals(c, listOf(4, 5, 4))
    c.remove(4)
    assertEquals(c, listOf(5, 4))
    c.clear()
    assertEquals(c, listOf())
}