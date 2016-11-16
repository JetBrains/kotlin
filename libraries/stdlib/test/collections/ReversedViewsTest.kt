/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.collections

import test.collections.behaviors.listBehavior
import test.collections.compare
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

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

    @Test fun testIndexOf() {
        assertEquals(2, listOf(1, 2, 3).asReversed().indexOf(1))
        assertEquals(2, mutableListOf(1, 2, 3).asReversed().indexOf(1))
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

    @Test fun testGetIOOB() {
        val success = try {
            listOf(1, 2, 3).asReversed().get(3)
            true
        } catch (expected: IndexOutOfBoundsException) {
            false
        }

        assertFalse(success)
    }

    @Test fun testSetIOOB() {
        val success = try {
            mutableListOf(1, 2, 3).asReversed().set(3, 0)
            true
        } catch(expected: IndexOutOfBoundsException) {
            false
        }

        assertFalse(success)
    }

    @Test fun testAddIOOB() {
        val success = try {
            mutableListOf(1, 2, 3).asReversed().add(4, 0)
            true
        } catch(expected: IndexOutOfBoundsException) {
            false
        }

        assertFalse(success)
    }

    @Test fun testRemoveIOOB() {
        val success = try {
            mutableListOf(1, 2, 3).asReversed().removeAt(3)
            true
        } catch(expected: IndexOutOfBoundsException) {
            false
        }

        assertFalse(success)
    }
}
