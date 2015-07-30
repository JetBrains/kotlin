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

package collections

import test.collections.behaviors.listBehavior
import test.collections.compare
import kotlin.asReversed
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test as test

class ReversedViewsTest {

    test fun testBehavior() {
        val original = listOf(2L, 3L, Long.MAX_VALUE)
        val reversed = original.reverse()
        compare(reversed, original.asReversed()) {
            listBehavior()
        }
    }

    test fun testSimple() {
        assertEquals(listOf(3, 2, 1), listOf(1, 2, 3).asReversed())
        assertEquals(listOf(3, 2, 1), listOf(1, 2, 3).asReversed().toList())
    }

    test fun testRandomAccess() {
        val reversed = listOf(1, 2, 3).asReversed()

        assertEquals(3, reversed[0])
        assertEquals(2, reversed[1])
        assertEquals(1, reversed[2])
    }

    test fun testDoubleReverse() {
        assertEquals(listOf(1, 2, 3), listOf(1, 2, 3).asReversed().asReversed())
        assertEquals(listOf(2, 3), listOf(1, 2, 3, 4).asReversed().subList(1, 3).asReversed())
    }

    test fun testEmpty() {
        assertEquals(emptyList<Int>(), emptyList<Int>().asReversed())
    }

    test fun testReversedSubList() {
        val reversed = (1..10).toList().asReversed()
        assertEquals(listOf(9, 8, 7), reversed.subList(1, 4))
    }

    test fun testMutableSimple() {
        assertEquals(listOf(3, 2, 1), arrayListOf(1, 2, 3).asReversed())
        assertEquals(listOf(3, 2, 1), arrayListOf(1, 2, 3).asReversed().toList())
    }

    test fun testMutableDoubleReverse() {
        assertEquals(listOf(1, 2, 3), arrayListOf(1, 2, 3).asReversed().asReversed())
        assertEquals(listOf(2, 3), arrayListOf(1, 2, 3, 4).asReversed().subList(1, 3).asReversed())
    }

    test fun testMutableEmpty() {
        assertEquals(emptyList<Int>(), arrayListOf<Int>().asReversed())
    }

    test fun testMutableReversedSubList() {
        val reversed = (1..10).toArrayList().asReversed()
        assertEquals(listOf(9, 8, 7), reversed.subList(1, 4))
    }

    test fun testMutableAdd() {
        val original = arrayListOf(1, 2, 3)
        val reversed = original.asReversed()

        reversed.add(0) // add zero at end of reversed
        assertEquals(listOf(3, 2, 1, 0), reversed)
        assertEquals(listOf(0, 1, 2, 3), original)

        reversed.add(0, 4) // add four at position 0
        assertEquals(listOf(4, 3, 2, 1, 0), reversed)
        assertEquals(listOf(0, 1, 2, 3, 4), original)
    }

    test fun testMutableSet() {
        val original = arrayListOf(1, 2, 3)
        val reversed = original.asReversed()

        reversed.set(0, 300)
        reversed.set(1, 200)
        reversed.set(2, 100)

        assertEquals(listOf(100, 200, 300), original)
        assertEquals(listOf(300, 200, 100), reversed)
    }

    test fun testMutableRemove() {
        val original = arrayListOf("a", "b", "c")
        val reversed = original.asReversed()

        reversed.remove(0) // remove c
        assertEquals(listOf("a", "b"), original)
        assertEquals(listOf("b", "a"), reversed)

        reversed.remove(1) // remove a
        assertEquals(listOf("b"), original)

        reversed.remove(0) // remove remaining b
        assertEquals(emptyList<String>(), original)
    }

    test fun testMutableRemoveByObj() {
        val original = arrayListOf("a", "b", "c")
        val reversed = original.asReversed()

        reversed.remove("c")
        assertEquals(listOf("a", "b"), original)
        assertEquals(listOf("b", "a"), reversed)
    }

    test fun testMutableClear() {
        val original = arrayListOf(1, 2, 3)
        val reversed = original.asReversed()

        reversed.clear()

        assertEquals(emptyList<Int>(), reversed)
        assertEquals(emptyList<Int>(), original)
    }

    test fun testContains() {
        assertTrue { 1 in listOf(1, 2, 3).asReversed() }
        assertTrue { 1 in arrayListOf(1, 2, 3).asReversed() }
    }

    test fun testIndexOf() {
        assertEquals(2, listOf(1, 2, 3).asReversed().indexOf(1))
        assertEquals(2, arrayListOf(1, 2, 3).asReversed().indexOf(1))
    }

    test fun testBidirectionalModifications() {
        val original = arrayListOf(1, 2, 3, 4)
        val reversed = original.asReversed()

        original.remove(3)
        assertEquals(listOf(1, 2, 3), original)
        assertEquals(listOf(3, 2, 1), reversed)

        reversed.remove(2)
        assertEquals(listOf(2, 3), original)
        assertEquals(listOf(3, 2), reversed)
    }

    test fun testGetIOOB() {
        val success = try {
            listOf(1, 2, 3).asReversed().get(3)
            true
        } catch (expected: IndexOutOfBoundsException) {
            false
        }

        assertFalse(success)
    }

    test fun testSetIOOB() {
        val success = try {
            arrayListOf(1, 2, 3).asReversed().set(3, 0)
            true
        } catch(expected: IndexOutOfBoundsException) {
            false
        }

        assertFalse(success)
    }

    test fun testAddIOOB() {
        val success = try {
            arrayListOf(1, 2, 3).asReversed().add(4, 0)
            true
        } catch(expected: IndexOutOfBoundsException) {
            false
        }

        assertFalse(success)
    }

    test fun testRemoveIOOB() {
        val success = try {
            arrayListOf(1, 2, 3).asReversed().remove(3)
            true
        } catch(expected: IndexOutOfBoundsException) {
            false
        }

        assertFalse(success)
    }
}
