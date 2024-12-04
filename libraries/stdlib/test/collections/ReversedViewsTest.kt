/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import test.collections.behaviors.listBehavior
import kotlin.test.*

class ReversedViewsTest {

    @Test fun testNullToString() {
        assertEquals("[null]", listOf<String?>(null).asReversed().toString())
    }

    @Test fun testBehavior() {
        val original = listOf(2L, 3L, Long.MAX_VALUE)
        val reversed = original.reversed()
        compare(reversed, original.asReversed()) {
            listBehavior()
        }
    }

    @Test fun testMutableBehavior() {
        val original = mutableListOf(2L, 3L, Long.MAX_VALUE)
        val reversed = original.reversed()
        compare(reversed, original.asReversed()) {
            listBehavior()
        }
    }

    @Test fun testSimple() {
        assertEquals(listOf(3, 2, 1), listOf(1, 2, 3).asReversed())
        assertEquals(listOf(3, 2, 1), listOf(1, 2, 3).asReversed().toList())
    }

    @Test fun testRandomAccess() {
        val reversed = listOf(1, 2, 3).asReversed()

        assertEquals(3, reversed[0])
        assertEquals(2, reversed[1])
        assertEquals(1, reversed[2])
    }

    @Test fun testDoubleReverse() {
        assertEquals(listOf(1, 2, 3), listOf(1, 2, 3).asReversed().asReversed())
        assertEquals(listOf(2, 3), listOf(1, 2, 3, 4).asReversed().subList(1, 3).asReversed())
    }

    @Test fun testEmpty() {
        assertEquals(emptyList<Int>(), emptyList<Int>().asReversed())
    }

    @Test fun testReversedSubList() {
        val reversed = (1..10).toList().asReversed()
        assertEquals(listOf(9, 8, 7), reversed.subList(1, 4))
    }

    @Test fun testMutableSubList() {
        val original = arrayListOf(1, 2, 3, 4)
        val reversedSubList = original.asReversed().subList(1, 3)

        assertEquals(listOf(3, 2), reversedSubList)
        reversedSubList.clear()
        assertEquals(emptyList<Int>(), reversedSubList)
        assertEquals(listOf(1, 4), original)

        reversedSubList.add(100)
        assertEquals(listOf(100), reversedSubList)
        assertEquals(listOf(1, 100, 4), original)
    }

    @Test fun testMutableSimple() {
        assertEquals(listOf(3, 2, 1), mutableListOf(1, 2, 3).asReversed())
        assertEquals(listOf(3, 2, 1), mutableListOf(1, 2, 3).asReversed().toList())
    }

    @Test fun testMutableDoubleReverse() {
        assertEquals(listOf(1, 2, 3), mutableListOf(1, 2, 3).asReversed().asReversed())
        assertEquals(listOf(2, 3), mutableListOf(1, 2, 3, 4).asReversed().subList(1, 3).asReversed())
    }

    @Test fun testMutableEmpty() {
        assertEquals(emptyList<Int>(), mutableListOf<Int>().asReversed())
    }

    @Test fun testMutableReversedSubList() {
        val reversed = (1..10).toMutableList().asReversed()
        assertEquals(listOf(9, 8, 7), reversed.subList(1, 4))
    }

    @Test fun testMutableAdd() {
        val original = mutableListOf(1, 2, 3)
        val reversed = original.asReversed()

        reversed.add(0) // add zero at end of reversed
        assertEquals(listOf(3, 2, 1, 0), reversed)
        assertEquals(listOf(0, 1, 2, 3), original)

        reversed.add(0, 4) // add four at position 0
        assertEquals(listOf(4, 3, 2, 1, 0), reversed)
        assertEquals(listOf(0, 1, 2, 3, 4), original)
    }

    @Test fun testMutableSet() {
        val original = mutableListOf(1, 2, 3)
        val reversed = original.asReversed()

        reversed.set(0, 300)
        reversed.set(1, 200)
        reversed.set(2, 100)

        assertEquals(listOf(100, 200, 300), original)
        assertEquals(listOf(300, 200, 100), reversed)
    }

    @Test fun testMutableRemove() {
        val original = mutableListOf("a", "b", "c")
        val reversed = original.asReversed()

        reversed.removeAt(0) // remove c
        assertEquals(listOf("a", "b"), original)
        assertEquals(listOf("b", "a"), reversed)

        reversed.removeAt(1) // remove a
        assertEquals(listOf("b"), original)

        reversed.removeAt(0) // remove remaining b
        assertEquals(emptyList<String>(), original)
    }

    @Test fun testMutableRemoveByObj() {
        val original = mutableListOf("a", "b", "c")
        val reversed = original.asReversed()

        reversed.remove("c")
        assertEquals(listOf("a", "b"), original)
        assertEquals(listOf("b", "a"), reversed)
    }

    @Test fun testMutableClear() {
        val original = mutableListOf(1, 2, 3)
        val reversed = original.asReversed()

        reversed.clear()

        assertEquals(emptyList<Int>(), reversed)
        assertEquals(emptyList<Int>(), original)
    }

    @Test fun testContains() {
        assertTrue { 1 in listOf(1, 2, 3).asReversed() }
        assertTrue { 1 in mutableListOf(1, 2, 3).asReversed() }
    }

    @Test fun testBidirectionalModifications() {
        val original = mutableListOf(1, 2, 3, 4)
        val reversed = original.asReversed()

        original.removeAt(3)
        assertEquals(listOf(1, 2, 3), original)
        assertEquals(listOf(3, 2, 1), reversed)

        reversed.removeAt(2)
        assertEquals(listOf(2, 3), original)
        assertEquals(listOf(3, 2), reversed)
    }

    @Test fun testIndexOf() {
        assertEquals(2, listOf(1, 2, 3).asReversed().indexOf(1))
        assertEquals(2, mutableListOf(1, 2, 3).asReversed().indexOf(1))

        assertEquals(-1, listOf(1, 2, 3).asReversed().indexOf(0))
        assertEquals(-1, mutableListOf(1, 2, 3).asReversed().indexOf(0))
    }

    @Test fun testLastIndexOf() {
        assertEquals(2, listOf(1, 2, 3).asReversed().indexOf(1))
        assertEquals(2, mutableListOf(1, 2, 3).asReversed().indexOf(1))

        assertEquals(-1, listOf(1, 2, 3).asReversed().lastIndexOf(0))
        assertEquals(-1, mutableListOf(1, 2, 3).asReversed().lastIndexOf(0))
    }

    @Test fun testIteratorAdd() {
        val original = mutableListOf(1, 2, 4)
        val reversedView = original.asReversed()
        val iter = reversedView.listIterator()

        val reversedCopy = original.reversed().toMutableList()
        val copyIter = reversedCopy.listIterator()

        compare(copyIter, iter) {
            propertyEquals { add(5) }
            propertyEquals { previousIndex() }
            propertyEquals { nextIndex() }
            propertyEquals { next() }
        }
        assertEquals(reversedCopy, reversedView)
        assertEquals(listOf(1, 2, 4, 5), original)

        compare(copyIter, iter) {
            propertyEquals { add(3) }
            propertyEquals { previousIndex() }
            propertyEquals { nextIndex() }
            propertyEquals { previous() }
        }

        iter.seekEnd()
        iter.add(0)
        assertEquals(listOf(5, 4, 3, 2, 1, 0), reversedView)
        assertEquals(listOf(0, 1, 2, 3, 4, 5), original)
    }

    @Test fun testIteratorRemove() {
        val original = mutableListOf(0, 1, 2, 3, 4)
        val reversedView = original.asReversed()
        val iter = reversedView.listIterator()

        val reversedCopy = original.reversed().toMutableList()
        val copyIter = reversedCopy.listIterator()

        compare(copyIter, iter) {
            propertyFailsWith<IllegalStateException> { remove() }
            propertyEquals {
                next()
                remove()
            }
            propertyEquals { previousIndex() }
            propertyEquals { nextIndex() }
            propertyFailsWith<IllegalStateException> { remove() }
            propertyEquals { next() }
        }
        assertEquals(reversedCopy, reversedView)
        assertEquals(listOf(0, 1, 2, 3), original)

        compare(copyIter, iter) {
            propertyEquals {
                next()
                remove()
            }
            propertyEquals { previousIndex() }
            propertyEquals { nextIndex() }
            propertyEquals { previous() }
        }
        assertEquals(reversedCopy, reversedView)
        assertEquals(listOf(0, 1, 3), original)

        iter.seekEnd()
        iter.remove()
        assertEquals(listOf(3, 1), reversedView)
        assertEquals(listOf(1, 3), original)
    }

    @Test fun testIteratorSet() {
        val original = mutableListOf(2, 3, 4)
        val iter = original.asReversed().listIterator()

        while (iter.hasNext()) {
            val v = iter.next()
            iter.set(v * v)
        }

        assertEquals(listOf(4, 9, 16), original)
    }

    @Test fun testGetIOOB() {
        assertFailsWith<IndexOutOfBoundsException> {
            listOf(1, 2, 3).asReversed().get(3)
        }.let { exception ->
            // we actually don't care about the exact wording,
            // we just need the index to be not confusing
            assertContains(exception.message!!, "index 3")
        }

        assertFailsWith<IndexOutOfBoundsException> {
            mutableListOf(1, 2, 3).asReversed().get(3)
        }.let { exception ->
            // we actually don't care about the exact wording,
            // we just need the index to be not confusing
            assertContains(exception.message!!, "index 3")
        }
    }

    @Test fun testSetIOOB() {
        assertFailsWith<IndexOutOfBoundsException> {
            mutableListOf(1, 2, 3, 4).asReversed().set(4, 0)
        }.let { exception ->
            // we actually don't care about the exact wording,
            // we just need the index to be not confusing
            assertContains(exception.message!!, "index 4")
        }
    }

    @Test fun testAddIOOB() {
        assertFailsWith<IndexOutOfBoundsException> {
            mutableListOf(1, 2, 3).asReversed().add(4, 0)
        }.let { exception ->
            // we actually don't care about the exact wording,
            // we just need the index to be not confusing
            assertContains(exception.message!!, "index 4")
        }
    }

    @Test fun testRemoveIOOB() {
        assertFailsWith<IndexOutOfBoundsException> {
            mutableListOf(1, 2, 3).asReversed().removeAt(3)
        }.let { exception ->
            // we actually don't care about the exact wording,
            // we just need the index to be not confusing
            assertContains(exception.message!!, "index 3")
        }
    }

    @Test fun testIteratorNSEOnNext() {
        assertFailsWith<NoSuchElementException> {
            val it = listOf(1, 2, 3).asReversed().iterator()
            it.seekEnd()
            it.next()
        }

        assertFailsWith<NoSuchElementException> {
            val it = mutableListOf(1, 2, 3).asReversed().iterator()
            it.seekEnd()
            it.next()
        }
    }

    @Test fun testIteratorNSEOnPrevious() {
        assertFailsWith<NoSuchElementException> {
            listOf(1, 2, 3).asReversed().listIterator().previous()
        }
        assertFailsWith<NoSuchElementException> {
            mutableListOf(1, 2, 3).asReversed().listIterator().previous()
        }
    }
}

private fun Iterator<*>.seekEnd() {
    while (hasNext()) {
        next()
    }
}
