/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import test.TestPlatform
import test.collections.js.linkedStringMapOf
import test.collections.js.linkedStringSetOf
import test.collections.js.stringMapOf
import test.collections.js.stringSetOf
import test.current
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail

class ConcurrentModificationTest {

    private fun <C, I : Iterator<*>> testIteratorThrowsCME(
        withCollection: WithCollection<C>,
        createIterator: C.() -> I,
        collectionOp: CollectionOperation<C>,
        iteratorOp: IteratorOperation<I>
    ) {
        var invoked = false
        withCollection { collection ->
            invoked = true

            val iterator = collection.createIterator()

            iteratorOp.precedingFunction?.invoke(iterator)
            collectionOp.function.invoke(collection)

            val message = "listOp: ${collectionOp.description}, iteratorOp: ${iteratorOp.description}"
            if (collectionOp.throwsCME) {
                assertFailsWith<ConcurrentModificationException>(message) {
                    iteratorOp.function.invoke(iterator)
                }
            } else {
                try {
                    iteratorOp.function.invoke(iterator)
                } catch (e: Throwable) {
                    fail("$message. Expected no exception, but was $e")
                }
            }
        }

        assertTrue(invoked)
    }

    private fun <C : MutableList<String>> testIteratorThrowsCME(
        withMutableList: WithCollection<C>,
        listOps: List<CollectionOperation<C>>
    ) {
        for (listOp in listOps) {
            for (iteratorOp in iteratorOperations<String>()) {
                testIteratorThrowsCME(withMutableList, { iterator() }, listOp, iteratorOp)
            }
            for (iteratorOp in listIteratorOperations) {
                testIteratorThrowsCME(withMutableList, { listIterator() }, listOp, iteratorOp)
                testIteratorThrowsCME(withMutableList, { listIterator(2) }, listOp, iteratorOp)
            }
        }
    }

    private fun <C : MutableList<String>> testSubListThrowsCME(
        withSubList: WithCollection<C>,
        subListOps: List<CollectionOperation<C>>
    ) {
        var invoked = false
        withSubList { subList ->
            invoked = true

            for (subListOp in subListOps) {
                val message = "subListOp: ${subListOp.description}"
                if (subListOp.throwsCME) {
                    assertFailsWith<ConcurrentModificationException>(message) {
                        subListOp.function.invoke(subList)
                    }
                } else {
                    try {
                        subListOp.function.invoke(subList)
                    } catch (e: Throwable) {
                        fail("$message. Expected no exception, but was $e")
                    }
                }
            }
        }
        assertTrue(invoked)
    }

    @Test
    fun mutableList() {
        if (TestPlatform.current == TestPlatform.Js) return

        /**
         * Some operations should register a modification by contract, but java ArrayList,
         * whose implementation we can't change, do not register.
         * @param isJavaArrayListBehavior specifies whether to test java ArrayList behavior or the behavior by contract.
         */
        fun operations(isJavaArrayListBehavior: Boolean) = listOf<CollectionOperation<MutableList<String>>>(
            CollectionOperation("set()", throwsCME = false) { set(2, "e") },

            CollectionOperation("add()") { add("e") },
            CollectionOperation("add(index)") { add(2, "e") },

            CollectionOperation("remove(non-existing)", throwsCME = false) { remove("e") },
            CollectionOperation("remove(existing)") { remove("d") },
            CollectionOperation("removeAt()") { removeAt(2) },

            CollectionOperation("addAll()") { addAll(listOf("e", "f")) },
            CollectionOperation("addAll(emptyList())", throwsCME = isJavaArrayListBehavior) { addAll(emptyList()) },
            CollectionOperation("addAll(index)") { addAll(2, listOf("e", "f")) },
            CollectionOperation("addAll(index, emptyList())", throwsCME = isJavaArrayListBehavior) { addAll(2, emptyList()) },

            CollectionOperation("removeAll(non-existing)", throwsCME = false) { removeAll(listOf("e", "f")) },
            CollectionOperation("removeAll(some exist)") { removeAll(listOf("d", "e")) },
            CollectionOperation("removeAll(emptyList())", throwsCME = false) { removeAll(emptyList()) },

            CollectionOperation("retainAll(this.toMutableList())", throwsCME = false) { retainAll(this.toMutableList()) },
            CollectionOperation("retainAll(non-existing)") { retainAll(listOf("e", "f")) },
            CollectionOperation("retainAll(some exist)") { retainAll(listOf("d", "e")) },
            CollectionOperation("retainAll(emptyList())") { retainAll(emptyList()) },

            CollectionOperation("clear()") { clear() },
            CollectionOperation("iterator.remove()") { iterator().apply { next(); remove() } },
        ).also { ops ->
            ops + ops.map {
                CollectionOperation("subList(1, size)." + it.description, it.throwsCME) { it.function.invoke(subList(1, size)) }
            }
        }

        fun testThrowsCME(isJavaArrayListBehavior: Boolean = true, withMutableList: WithCollection<MutableList<String>>) {
            testIteratorThrowsCME(withMutableList, operations(isJavaArrayListBehavior))
        }

        // size == capacity
        testThrowsCME { action ->
            MutableList(4) { ('a' + it).toString() }.also(action)
        }
        testThrowsCME { action ->
            mutableListOf("a", "b", "c", "d").also(action)
        }

        testThrowsCME { action ->
            buildList(4) {
                addAll(listOf("a", "b", "c", "d"))
                action(this)
            }
        }

        testThrowsCME(isJavaArrayListBehavior = false) { action ->
            ArrayDeque<String>(4).apply {
                addAll(listOf("a", "b", "c", "d"))
                action(this)
            }
        }
        testThrowsCME(isJavaArrayListBehavior = false) { action ->
            ArrayDeque(listOf("a", "b", "c", "d")).also(action)
        }

        // size < capacity
        testThrowsCME { action ->
            ArrayList<String>(10).apply {
                addAll(listOf("a", "b", "c", "d"))
                action(this)
            }
        }

        testThrowsCME { action ->
            buildList(10) {
                addAll(listOf("a", "b", "c", "d"))
                action(this)
            }
        }

        testThrowsCME(isJavaArrayListBehavior = false) { action ->
            ArrayDeque<String>(10).apply {
                addAll(listOf("a", "b", "c", "d"))
                action(this)
            }
        }
    }

    @Test
    fun arrayList() {
        if (TestPlatform.current == TestPlatform.Js) return

        val operations = listOf<CollectionOperation<ArrayList<String>>>(
            CollectionOperation("trimToSize()") { trimToSize() },
            CollectionOperation("ensureCapacity(minCapacity > capacity)") { ensureCapacity(100) },
        )

        fun testThrowsCME(withArrayList: WithCollection<ArrayList<String>>) {
            testIteratorThrowsCME(withArrayList, operations)
        }

        // size == capacity
        testThrowsCME { action ->
            ArrayList<String>(4).apply {
                addAll(listOf("a", "b", "c", "d"))
                action(this)
            }
        }
        testThrowsCME { action ->
            arrayListOf("a", "b", "c", "d").also(action)
        }

        // size < capacity
        testThrowsCME { action ->
            ArrayList<String>(10).apply {
                addAll(listOf("a", "b", "c", "d"))
                action(this)
            }
        }
    }

    @Test
    fun arrayDeque() {
        if (TestPlatform.current == TestPlatform.Js) return

        val operations = listOf<CollectionOperation<ArrayDeque<String>>>(
            CollectionOperation("addFirst()") { addFirst("e") },
            CollectionOperation("addLast()") { addLast("e") },

            CollectionOperation("removeFirst()") { removeFirst() },
            CollectionOperation("removeLast()") { removeLast() },

            CollectionOperation("removeFirstOrNull()") { removeFirstOrNull() },
            CollectionOperation("removeLastOrNull()") { removeLastOrNull() },
        )

        fun testThrowsCME(withArrayDeque: WithCollection<ArrayDeque<String>>) {
            testIteratorThrowsCME(withArrayDeque, operations)
        }

        // size == capacity
        testThrowsCME { action ->
            ArrayDeque<String>(4).apply {
                addAll(listOf("a", "b", "c", "d"))
                action(this)
            }
        }
        testThrowsCME { action ->
            ArrayDeque(listOf("a", "b", "c", "d")).also(action)
        }

        // size < capacity
        testThrowsCME { action ->
            ArrayDeque<String>(10).apply {
                addAll(listOf("a", "b", "c", "d"))
                action(this)
            }
        }
    }

    @Test
    fun subList() {
        if (TestPlatform.current == TestPlatform.Js) return

        val operations = listOf<CollectionOperation<MutableList<String>>>(
            CollectionOperation("isEmpty()") { isEmpty() },
            CollectionOperation("size") { size },

            CollectionOperation("equals()") { equals(listOf("x")) },
            CollectionOperation("hashCode()") { hashCode() },
            CollectionOperation("toString()") { toString() },

            CollectionOperation("indexOf") { indexOf("d") },
            CollectionOperation("lastIndexOf") { lastIndexOf("d") },
            CollectionOperation("contains") { contains("d") },

            CollectionOperation("get()") { get(2) },
            CollectionOperation("set()") { set(2, "e") },

            CollectionOperation("add()") { add("e") },
            CollectionOperation("add(index)") { add(2, "e") },

            CollectionOperation("remove()") { remove("d") },
            CollectionOperation("removeAt()") { removeAt(2) },

            CollectionOperation("addAll()") { addAll(listOf("e", "f")) },
            CollectionOperation("addAll(index)") { addAll(2, listOf("e", "f")) },

            CollectionOperation("removeAll()") { removeAll(listOf("d", "e")) },

            CollectionOperation("retainAll()") { retainAll(listOf("d", "e")) },

            CollectionOperation("clear()") { clear() },
            CollectionOperation("iterator()") { iterator() },
            CollectionOperation("listIterator()") { listIterator() },
        )

        fun testThrowsCME(withMutableList: WithCollection<MutableList<String>>) {
            testSubListThrowsCME(withMutableList, operations)
        }

        testThrowsCME { action ->
            val arrayList = arrayListOf("a", "b", "c", "d")
            val subList = arrayList.subList(0, arrayList.size)
            arrayList.add("e")
            action(subList)
        }
        assertFailsWith<ConcurrentModificationException> {
            val arrayList = arrayListOf("a", "b", "c", "d")
            for (e in arrayList.subList(1, 3)) arrayList.remove(e)
        }

        testThrowsCME { action ->
            buildList {
                addAll(listOf("a", "b", "c", "d"))
                val subList = subList(0, size)
                add("e")
                action(subList)
            }
        }
        assertFailsWith<ConcurrentModificationException> {
            buildList<String> {
                addAll(listOf("a", "b", "c"))
                for (e in subList(1, 3)) remove(e)
            }
        }

        testThrowsCME { action ->
            val arrayDeque = ArrayDeque(listOf("a", "b", "c", "d"))
            val subList = arrayDeque.subList(0, arrayDeque.size)
            arrayDeque.add("e")
            action(subList)
        }
        assertFailsWith<ConcurrentModificationException> {
            val arrayDeque = ArrayDeque(listOf("a", "b", "c", "d"))
            for (e in arrayDeque.subList(1, 3)) arrayDeque.remove(e)
        }
    }

    @Test
    fun mutableSet() {
        val operations = listOf<CollectionOperation<MutableSet<String>>>(
            CollectionOperation("add(non-existing)") { add("e") },
            CollectionOperation("add(existing)", throwsCME = false) { add("d") },

            CollectionOperation("remove(non-existing)", throwsCME = false) { remove("e") },
            CollectionOperation("remove(existing)") { remove("d") },

            CollectionOperation("addAll(emptyList())", throwsCME = false) { addAll(emptyList()) },
            CollectionOperation("addAll(existing)", throwsCME = false) { addAll(listOf("d", "b")) },
            CollectionOperation("addAll(some exist)") { addAll(listOf("d", "e")) },
            CollectionOperation("addAll(non-existing)") { addAll(listOf("e", "f")) },

            CollectionOperation("removeAll(non-existing)", throwsCME = false) { removeAll(listOf("e", "f")) },
            CollectionOperation("removeAll(some exist") { removeAll(listOf("d", "e")) },
            CollectionOperation("removeAll(emptyList())", throwsCME = false) { removeAll(emptyList()) },

            CollectionOperation("retainAll(this.toMutableSet())", throwsCME = false) { retainAll(this.toMutableSet()) },
            CollectionOperation("retainAll(non-existing)") { retainAll(listOf("e", "f")) },
            CollectionOperation("retainAll(some exist)") { retainAll(listOf("d", "e")) },
            CollectionOperation("retainAll(emptyList())") { retainAll(emptyList()) },

            CollectionOperation("clear()") { clear() },
            CollectionOperation("iterator.remove()") { iterator().apply { next(); remove() } },
        )

        fun testThrowsCME(withMutableSet: WithCollection<MutableSet<String>>) {
            for (setOp in operations) {
                for (iteratorOp in iteratorOperations<String>()) {
                    testIteratorThrowsCME(withMutableSet, { iterator() }, setOp, iteratorOp)
                }
            }
        }

        // Because platform implementations may have different load factor and rehash strategy,
        // make sure there is enough capacity to avoid rehash.

        val elements = listOf("a", "b", "c", "d")

        testThrowsCME { action ->
            LinkedHashSet<String>(10).apply {
                addAll(elements)
                action(this)
            }
        }
        testThrowsCME { action ->
            HashSet<String>(10).apply {
                addAll(elements)
                action(this)
            }
        }

        testThrowsCME { action ->
            buildSet(10) {
                addAll(elements)
                action(this)
            }
        }

        if (TestPlatform.current == TestPlatform.Js) {
            testThrowsCME { action ->
                stringSetOf().apply {
                    addAll(elements)
                    action(this)
                }
            }
            testThrowsCME { action ->
                linkedStringSetOf().apply {
                    addAll(elements)
                    action(this)
                }
            }
        }
    }

    @Test
    fun mutableMap() {
        val operations = listOf<CollectionOperation<MutableMap<String, String>>>(
            CollectionOperation("put(non-existing)") { put("e", "e") },
            CollectionOperation("put(existing)", throwsCME = false) { put("d", "d") },
            CollectionOperation("put(update value)", throwsCME = false) { put("d", "D") },

            CollectionOperation("remove(non-existing)", throwsCME = false) { remove("e") },
            CollectionOperation("remove(existing)") { remove("d") },

            CollectionOperation("putAll(emptyList())", throwsCME = false) { putAll(emptyMap()) },
            CollectionOperation("putAll(existing)", throwsCME = false) { putAll(mapOf("d" to "d", "b" to "b")) },
            CollectionOperation("putAll(update values)", throwsCME = false) { putAll(mapOf("d" to "D", "b" to "B")) },
            CollectionOperation("putAll(some exist)") { putAll(mapOf("d" to "d", "e" to "e")) },
            CollectionOperation("putAll(non-existing)") { putAll(mapOf("e" to "e", "f" to "f")) },

            CollectionOperation("clear()") { clear() },
            CollectionOperation("iterator.remove()") { iterator().apply { next(); remove() } },
        )

        fun testThrowsCME(withMutableMap: WithCollection<MutableMap<String, String>>) {
            for (mapOp in operations) {
                for (iteratorOp in iteratorOperations<String>()) {
                    testIteratorThrowsCME(withMutableMap, { keys.iterator() }, mapOp, iteratorOp)
                    testIteratorThrowsCME(withMutableMap, { values.iterator() }, mapOp, iteratorOp)
                }
                for (iteratorOp in iteratorOperations<MutableMap.MutableEntry<String, String>>()) {
                    testIteratorThrowsCME(withMutableMap, { entries.iterator() }, mapOp, iteratorOp)
                }
            }
        }

        // Because platform implementations may have different load factor and rehash strategy,
        // make sure there is enough capacity to avoid rehash.

        val entries = mapOf("a" to "a", "b" to "b", "c" to "c", "d" to "d")

        testThrowsCME { action ->
            LinkedHashMap<String, String>(10).apply {
                putAll(entries)
                action(this)
            }
        }
        testThrowsCME { action ->
            HashMap<String, String>(10).apply {
                putAll(entries)
                action(this)
            }
        }
        testThrowsCME { action ->
            buildMap(10) {
                putAll(entries)
                action(this)
            }
        }

        if (TestPlatform.current == TestPlatform.Js) {
            testThrowsCME { action ->
                stringMapOf<String>().apply {
                    putAll(entries)
                    action(this)
                }
            }
            testThrowsCME { action ->
                linkedStringMapOf<String>().apply {
                    putAll(entries)
                    action(this)
                }
            }
        }
    }
}

private typealias WithCollection<C> = (action: (C) -> Unit) -> Unit

private class CollectionOperation<C>(
    val description: String,
    val throwsCME: Boolean = true,
    val function: C.() -> Unit
)

private class IteratorOperation<I>(
    val description: String,
    val precedingFunction: (I.() -> Unit)? = null,
    val function: I.() -> Unit
)

private fun <E> iteratorOperations() = listOf<IteratorOperation<MutableIterator<E>>>(
    IteratorOperation("next()") { next() },
    IteratorOperation("remove()", { next() }) { remove() }
)
private val listIteratorOperations = listOf<IteratorOperation<MutableListIterator<String>>>(
    IteratorOperation("next()") { next() },
    IteratorOperation("remove()", { next() }) { remove() },
    IteratorOperation("previous()", { next() }) { previous() },
    IteratorOperation("add(\"e\")") { add("e") }
)
