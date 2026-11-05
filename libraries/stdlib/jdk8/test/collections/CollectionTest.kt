/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package kotlin.jdk8.collections.test

import kotlin.test.Test
import kotlin.test.*
import java.util.function.Predicate
import java.util.stream.Collectors
import kotlin.streams.*

class CollectionTest {

    val data = listOf("abc", "fo", "baar")

    @Test fun stream() {
        assertEquals(
            data.flatMap { it.asIterable() },
            data.stream()
                .flatMap { it.chars().boxed().map { it.toChar() } }
                .collect(Collectors.toList()))

        assertEquals(data, data.parallelStream().toList())
    }


    @Test fun removeIf() {
        val coll: MutableCollection<String> = data.toMutableList()
        assertTrue(coll.removeIf { it.length < 3 })
        assertEquals(listOf("abc", "baar"), coll as Collection<String>)
        assertFalse(coll.removeIf(Predicate { it.length > 4 }))
    }

    private class RemoveIfRecordingCollection(elements: List<String>) : AbstractMutableCollection<String>() {
        private val backingList = elements.toMutableList()

        val removeIfInvocationCount
            get() = removeIfPredicates.size

        val removeIfPredicates: List<Predicate<in String>>
            field = mutableListOf()

        override val size: Int
            get() = backingList.size

        override fun add(element: String): Boolean = backingList.add(element)

        override fun iterator(): MutableIterator<String> = backingList.iterator()

        override fun removeIf(filter: Predicate<in String>): Boolean {
            removeIfPredicates.add(filter)
            return backingList.removeIf(filter)
        }
    }

    private class RemoveIfRecordingList(elements: List<String>) : AbstractMutableList<String>() {
        private val backingList = elements.toMutableList()

        var removeIfInvocationCount = 0
            private set

        override val size: Int
            get() = backingList.size

        override fun get(index: Int): String = backingList[index]

        override fun set(index: Int, element: String): String = backingList.set(index, element)

        override fun add(index: Int, element: String) {
            backingList.add(index, element)
        }

        override fun removeAt(index: Int): String = backingList.removeAt(index)

        override fun removeIf(filter: Predicate<in String>): Boolean {
            removeIfInvocationCount++
            return backingList.removeIf(filter)
        }
    }

    @Test fun removeAllDelegatesToCollectionRemoveIf() {
        val collection = RemoveIfRecordingCollection(listOf("foo", "bar", "bar"))
        val iterable: MutableIterable<String> = collection

        assertTrue(iterable.removeAll { it == "bar" })
        assertEquals(1, collection.removeIfInvocationCount)
        assertEquals(listOf("foo"), collection.toList())

        assertFalse(iterable.removeAll { it == "bar" })
        assertEquals(2, collection.removeIfInvocationCount)
        assertEquals(listOf("foo"), collection.toList())
    }

    @Test fun retainAllDelegatesToCollectionRemoveIfWithNegatedPredicate() {
        val collection = RemoveIfRecordingCollection(listOf("foo", "bar", "bar"))
        val iterable: MutableIterable<String> = collection

        assertTrue(iterable.retainAll { it == "foo" })
        assertEquals(1, collection.removeIfInvocationCount)
        assertEquals(listOf("foo"), collection.toList())
        collection.removeIfPredicates.last().let { predicate ->
            assertFalse(predicate.test("foo"))
            assertTrue(predicate.test("whatever"))
        }

        assertFalse(iterable.retainAll { it == "foo" })
        assertEquals(2, collection.removeIfInvocationCount)
        assertEquals(listOf("foo"), collection.toList())
        collection.removeIfPredicates.last().let { predicate ->
            assertFalse(predicate.test("foo"))
            assertTrue(predicate.test("whatever"))
        }
    }

    @Test fun removeAllFromMutableListDelegatesToCollectionRemoveIf() {
        val list = RemoveIfRecordingList(listOf("foo", "bar", "bar"))
        val mutableList: MutableList<String> = list

        assertTrue(mutableList.removeAll { it == "bar" })
        assertEquals(1, list.removeIfInvocationCount)
        assertEquals(listOf("foo"), list)

        assertFalse(mutableList.removeAll { it == "bar" })
        assertEquals(2, list.removeIfInvocationCount)
        assertEquals(listOf("foo"), list)
    }
}
