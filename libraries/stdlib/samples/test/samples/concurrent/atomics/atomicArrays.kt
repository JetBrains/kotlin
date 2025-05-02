/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.concurrent.atomics

import kotlin.concurrent.atomics.*
import samples.*
import kotlin.concurrent.atomics.AtomicArray
import kotlin.test.assertFailsWith

@OptIn(ExperimentalAtomicApi::class)
class AtomicIntArray {
    @Sample
    fun sizeCons() {
        val a = AtomicIntArray(5)
        assertPrints(a.toString(), "[0, 0, 0, 0, 0]")
    }

    @Sample
    fun intArrCons() {
        val a = AtomicIntArray(intArrayOf(1, 2, 3))
        assertPrints(a.toString(), "[1, 2, 3]")
    }

    @Sample
    fun initCons() {
        val a = AtomicIntArray(3) { it * 10 }
        assertPrints(a.toString(), "[0, 10, 20]")
    }

    @Sample
    fun factory() {
        val array = atomicIntArrayOf(1, 2, 3)
        assertPrints(array.toString(), "[1, 2, 3]")

        assertPrints(atomicIntArrayOf().toString(), "[]")
    }

    @Sample
    fun size() {
        val a = AtomicIntArray(intArrayOf(1, 2, 3, 4, 5))
        assertPrints(a.size, "5")
    }

    @Sample
    fun loadAt() {
        val a = AtomicIntArray(intArrayOf(0, 10, 20))
        assertPrints(a.loadAt(1), "10")
    }

    @Sample
    fun storeAt() {
        val a = AtomicIntArray(3)
        a.storeAt(1, 7)
        assertPrints(a.loadAt(1), "7")
        assertPrints(a.toString(), "[0, 7, 0]")
    }

    @Sample
    fun exchangeAt() {
        val a = AtomicIntArray(intArrayOf(1, 2, 3))
        assertPrints(a.exchangeAt(1, 7), "2")
        assertPrints(a.loadAt(1), "7")
        assertPrints(a.toString(), "[1, 7, 3]")
    }

    @Sample
    fun compareAndSetAt() {
        val a = AtomicIntArray(intArrayOf(1, 2, 3))
        // Current value of a[1] is 2, it is equal to the expected value 2 -> compareAndSetAt succeeds.
        assertPrints(a.compareAndSetAt(index = 1, expectedValue = 2, newValue = 7), "true")
        assertPrints(a.loadAt(1), "7")
        assertPrints(a.toString(), "[1, 7, 3]")

        // Current value of a[2] is 3, it is not equal to the expected value 4 -> compareAndSetAt fails.
        assertPrints(a.compareAndSetAt(index = 2, expectedValue = 4, newValue = 25), "false")
        assertPrints(a.loadAt(2), "3")
        assertPrints(a.toString(), "[1, 7, 3]")
    }

    @Sample
    fun compareAndExchangeAt() {
        val a = AtomicIntArray(intArrayOf(1, 2, 3))
        // Current value of a[1] is 2, it is equal to the expected value 2 ->
        // compareAndExchangeAt succeeds, stores the new value 7 to a[1] and returns the old value 2.
        assertPrints(a.compareAndExchangeAt(index = 1, expectedValue = 2, newValue = 7), "2")
        assertPrints(a.loadAt(1), "7")
        assertPrints(a.toString(), "[1, 7, 3]")

        // Current value of a[2] is 3, it is not equal to the expected value 4 ->
        // compareAndExchangeAt fails, does not store the new value to a[2] and returns the current value 3.
        assertPrints(a.compareAndExchangeAt(index = 2, expectedValue = 4, newValue = 25), "3")
        assertPrints(a.loadAt(2), "3")
        assertPrints(a.toString(), "[1, 7, 3]")
    }

    @Sample
    fun fetchAndAddAt() {
        val a = AtomicIntArray(intArrayOf(1, 2, 3))
        // Returns the old value before the addition.
        assertPrints(a.fetchAndAddAt(1, 10), "2")
        assertPrints(a.loadAt(1), "12")
        assertPrints(a.toString(), "[1, 12, 3]")
    }

    @Sample
    fun addAndFetchAt() {
        val a = AtomicIntArray(intArrayOf(1, 2, 3))
        // Returns the new value after the addition.
        assertPrints(a.addAndFetchAt(1, 10), "12")
        assertPrints(a.loadAt(1), "12")
        assertPrints(a.toString(), "[1, 12, 3]")
    }

    @Sample
    fun fetchAndIncrementAt() {
        val a = AtomicIntArray(intArrayOf(1, 2, 3))
        assertPrints(a.fetchAndIncrementAt(1), "2")
        assertPrints(a.loadAt(1), "3")
        assertPrints(a.toString(), "[1, 3, 3]")
    }

    @Sample
    fun incrementAndFetchAt() {
        val a = AtomicIntArray(intArrayOf(1, 2, 3))
        assertPrints(a.incrementAndFetchAt(1), "3")
        assertPrints(a.loadAt(1), "3")
        assertPrints(a.toString(), "[1, 3, 3]")
    }

    @Sample
    fun fetchAndDecrementAt() {
        val a = AtomicIntArray(intArrayOf(1, 2, 3))
        assertPrints(a.fetchAndDecrementAt(1), "2")
        assertPrints(a.loadAt(1), "1")
        assertPrints(a.toString(), "[1, 1, 3]")
    }

    @Sample
    fun decrementAndFetchAt() {
        val a = AtomicIntArray(intArrayOf(1, 2, 3))
        assertPrints(a.decrementAndFetchAt(1), "1")
        assertPrints(a.loadAt(1), "1")
        assertPrints(a.toString(), "[1, 1, 3]")
    }

    @Sample
    fun updateAt() {
        val a = atomicIntArrayOf(1, 2, 3)
        a.updateAt(1) { currentValue -> currentValue * 10 }
        assertPrints(a.toString(), "[1, 20, 3]")

        // The index is out of array bounds
        assertFailsWith<IndexOutOfBoundsException> { a.updateAt(10) { it } }
    }

    @Sample
    fun updateAndFetchAt() {
        val a = atomicIntArrayOf(1, 2, 3)
        val updatedValue = a.updateAndFetchAt(1) { currentValue -> currentValue * 10 }
        assertPrints(updatedValue, "20")
        assertPrints(a.toString(), "[1, 20, 3]")

        // The index is out of array bounds
        assertFailsWith<IndexOutOfBoundsException> { a.updateAndFetchAt(10) { it } }
    }

    @Sample
    fun fetchAndUpdateAt() {
        val a = atomicIntArrayOf(1, 2, 3)
        val oldValue = a.fetchAndUpdateAt(1) { currentValue -> currentValue * 10 }
        assertPrints(oldValue, "2")
        assertPrints(a.toString(), "[1, 20, 3]")

        // The index is out of array bounds
        assertFailsWith<IndexOutOfBoundsException> { a.fetchAndUpdateAt(10) { it } }
    }
}

@OptIn(ExperimentalAtomicApi::class)
class AtomicLongArray {
    @Sample
    fun sizeCons() {
        val a = AtomicLongArray(5)
        assertPrints(a.toString(), "[0, 0, 0, 0, 0]")
    }

    @Sample
    fun longArrCons() {
        val a = AtomicLongArray(longArrayOf(1, 2, 3))
        assertPrints(a.toString(), "[1, 2, 3]")
    }

    @Sample
    fun initCons() {
        val a = AtomicLongArray(3) { it * 10L }
        assertPrints(a.toString(), "[0, 10, 20]")
    }

    @Sample
    fun factory() {
        val array = atomicLongArrayOf(1L, 2L, 3L)
        assertPrints(array.toString(), "[1, 2, 3]")

        assertPrints(atomicLongArrayOf().toString(), "[]")
    }

    @Sample
    fun size() {
        val a = AtomicLongArray(longArrayOf(1, 2, 3, 4, 5))
        assertPrints(a.size, "5")
    }

    @Sample
    fun loadAt() {
        val a = AtomicLongArray(longArrayOf(0, 10, 20))
        assertPrints(a.loadAt(1), "10")
        assertPrints(a.toString(), "[0, 10, 20]")
    }

    @Sample
    fun storeAt() {
        val a = AtomicLongArray(3)
        a.storeAt(1, 7)
        assertPrints(a.loadAt(1), "7")
        assertPrints(a.toString(), "[0, 7, 0]")
    }

    @Sample
    fun exchangeAt() {
        val a = AtomicLongArray(longArrayOf(1, 2, 3))
        assertPrints(a.exchangeAt(1, 7), "2")
        assertPrints(a.loadAt(1), "7")
        assertPrints(a.toString(), "[1, 7, 3]")
    }

    @Sample
    fun compareAndSetAt() {
        val a = AtomicLongArray(longArrayOf(1, 2, 3))
        // Current value of a[1] is 2, it is equal to the expected value 2 -> compareAndSetAt succeeds.
        assertPrints(a.compareAndSetAt(index = 1, expectedValue = 2, newValue = 7), "true")
        assertPrints(a.loadAt(1), "7")
        assertPrints(a.toString(), "[1, 7, 3]")

        // Current value of a[2] is 3, it is not equal to the expected value 4 -> compareAndSetAt fails.
        assertPrints(a.compareAndSetAt(index = 2, expectedValue = 4, newValue = 25), "false")
        assertPrints(a.loadAt(2), "3")
        assertPrints(a.toString(), "[1, 7, 3]")
    }

    @Sample
    fun compareAndExchangeAt() {
        val a = AtomicLongArray(longArrayOf(1, 2, 3))
        // Current value of a[1] is 2, it is equal to the expected value 2 ->
        // compareAndExchangeAt succeeds, stores the new value 7 to a[1] and returns the old value 2.
        assertPrints(a.compareAndExchangeAt(index = 1, expectedValue = 2, newValue = 7), "2")
        assertPrints(a.loadAt(1), "7")
        assertPrints(a.toString(), "[1, 7, 3]")

        // Current value of a[2] is 3, it is not equal to the expected value 4 ->
        // compareAndExchangeAt fails, does not store the new value to a[2] and returns the current value 3.
        assertPrints(a.compareAndExchangeAt(index = 2, expectedValue = 4, newValue = 25), "3")
        assertPrints(a.loadAt(2), "3")
        assertPrints(a.toString(), "[1, 7, 3]")
    }

    @Sample
    fun fetchAndAddAt() {
        val a = AtomicLongArray(longArrayOf(1, 2, 3))
        // Returns the old value before the addition.
        assertPrints(a.fetchAndAddAt(1, 10), "2")
        assertPrints(a.loadAt(1), "12")
        assertPrints(a.toString(), "[1, 12, 3]")
    }

    @Sample
    fun addAndFetchAt() {
        val a = AtomicLongArray(longArrayOf(1, 2, 3))
        // Returns the new value after the addition.
        assertPrints(a.addAndFetchAt(1, 10), "12")
        assertPrints(a.loadAt(1), "12")
        assertPrints(a.toString(), "[1, 12, 3]")
    }

    @Sample
    fun fetchAndIncrementAt() {
        val a = AtomicLongArray(longArrayOf(1, 2, 3))
        assertPrints(a.fetchAndIncrementAt(1), "2")
        assertPrints(a.loadAt(1), "3")
        assertPrints(a.toString(), "[1, 3, 3]")
    }

    @Sample
    fun incrementAndFetchAt() {
        val a = AtomicLongArray(longArrayOf(1, 2, 3))
        assertPrints(a.incrementAndFetchAt(1), "3")
        assertPrints(a.loadAt(1), "3")
        assertPrints(a.toString(), "[1, 3, 3]")
    }

    @Sample
    fun fetchAndDecrementAt() {
        val a = AtomicLongArray(longArrayOf(1, 2, 3))
        assertPrints(a.fetchAndDecrementAt(1), "2")
        assertPrints(a.loadAt(1), "1")
        assertPrints(a.toString(), "[1, 1, 3]")
    }

    @Sample
    fun decrementAndFetchAt() {
        val a = AtomicLongArray(longArrayOf(1, 2, 3))
        assertPrints(a.decrementAndFetchAt(1), "1")
        assertPrints(a.loadAt(1), "1")
        assertPrints(a.toString(), "[1, 1, 3]")
    }

    @Sample
    fun updateAt() {
        val a = atomicLongArrayOf(1L, 2L, 3L)
        a.updateAt(1) { currentValue -> currentValue * 10L }
        assertPrints(a.toString(), "[1, 20, 3]")

        // The index is out of array bounds
        assertFailsWith<IndexOutOfBoundsException> { a.updateAt(10) { it } }
    }

    @Sample
    fun updateAndFetchAt() {
        val a = atomicLongArrayOf(1L, 2L, 3L)
        val updatedValue = a.updateAndFetchAt(1) { currentValue -> currentValue * 10L }
        assertPrints(updatedValue, "20")
        assertPrints(a.toString(), "[1, 20, 3]")

        // The index is out of array bounds
        assertFailsWith<IndexOutOfBoundsException> { a.updateAndFetchAt(10) { it } }
    }

    @Sample
    fun fetchAndUpdateAt() {
        val a = atomicLongArrayOf(1L, 2L, 3L)
        val oldValue = a.fetchAndUpdateAt(1) { currentValue -> currentValue * 10L }
        assertPrints(oldValue, "2")
        assertPrints(a.toString(), "[1, 20, 3]")

        // The index is out of array bounds
        assertFailsWith<IndexOutOfBoundsException> { a.fetchAndUpdateAt(10) { it } }
    }
}

@OptIn(ExperimentalAtomicApi::class)
class AtomicArray {
    @Sample
    fun arrCons() {
        val a = AtomicArray(arrayOf("aaa", "bbb", "ccc"))
        assertPrints(a.toString(), "[aaa, bbb, ccc]")
    }

    @Sample
    fun initCons() {
        val a = AtomicArray(3) { "a$it" }
        assertPrints(a.toString(), "[a0, a1, a2]")

        // Size should be non-negative
        assertFailsWith<RuntimeException> { AtomicArray(-1) { "$it" } }
    }

    @Sample
    fun factory() {
        val array = atomicArrayOf("a", "b", "c")
        assertPrints(array.toString(), "[a, b, c]")

        val emptyArray = atomicArrayOf<String>()
        assertPrints(emptyArray.toString(), "[]")
    }

    @Sample
    fun nullFactory() {
        val array = atomicArrayOfNulls<String>(3)
        assertPrints(array.toString(), "[null, null, null]")

        assertPrints(atomicArrayOfNulls<String>(0).toString(), "[]")

        // Size should be non-negative
        assertFailsWith<RuntimeException> { atomicArrayOfNulls<String>(-1) }
    }

    @Sample
    fun size() {
        val a = AtomicArray(arrayOf("a", "b", "c", "d", "e"))
        assertPrints(a.size, "5")
    }

    @Sample
    fun loadAt() {
        val a = AtomicArray(arrayOf("aaa", "bbb", "ccc"))
        assertPrints(a.loadAt(1), "bbb")
        assertPrints(a.toString(), "[aaa, bbb, ccc]")
    }

    @Sample
    fun storeAt() {
        val a = AtomicArray(arrayOf("aaa", "bbb", "ccc"))
        a.storeAt(1, "kkk")
        assertPrints(a.loadAt(1), "kkk")
        assertPrints(a.toString(), "[aaa, kkk, ccc]")
    }

    @Sample
    fun exchangeAt() {
        val a = AtomicArray(arrayOf("aaa", "bbb", "ccc"))
        assertPrints(a.exchangeAt(1, "kkk"), "bbb")
        assertPrints(a.loadAt(1), "kkk")
        assertPrints(a.toString(), "[aaa, kkk, ccc]")
    }

    @Sample
    fun compareAndSetAt() {
        val a = AtomicArray(arrayOf("aaa", "bbb", "ccc"))
        // Current value of a[1] is "bbb", it is equal to the expected value "bbb" -> compareAndSetAt succeeds.
        assertPrints(a.compareAndSetAt(index = 1, expectedValue = "bbb", newValue = "kkk"), "true")
        assertPrints(a.loadAt(1), "kkk")
        assertPrints(a.toString(), "[aaa, kkk, ccc]")

        // Current value of a[2] is "ccc", it is not equal to the expected value "aaa" -> compareAndSetAt fails.
        assertPrints(a.compareAndSetAt(index = 2, expectedValue = "aaa", newValue = "jjj"), "false")
        assertPrints(a.loadAt(2), "ccc")
        assertPrints(a.toString(), "[aaa, kkk, ccc]")
    }

    @Sample
    fun compareAndExchangeAt() {
        val a = AtomicArray(arrayOf("aaa", "bbb", "ccc"))
        // Current value of a[1] is "bbb, it is equal to the expected value "bbb" ->
        // compareAndExchangeAt succeeds, stores the new value "kkk" to a[1] and returns the old value "bbb".
        assertPrints(a.compareAndExchangeAt(index = 1, expectedValue = "bbb", newValue = "kkk"), "bbb")
        assertPrints(a.loadAt(1), "kkk")
        assertPrints(a.toString(), "[aaa, kkk, ccc]")

        // Current value of a[2] is "ccc", it is not equal to the expected value "aaa" ->
        // compareAndExchangeAt fails, does not store the new value to a[2] and returns the current value "ccc".
        assertPrints(a.compareAndExchangeAt(index = 2, expectedValue = "aaa", newValue = "jjj"), "ccc")
        assertPrints(a.loadAt(2), "ccc")
        assertPrints(a.toString(), "[aaa, kkk, ccc]")
    }

    @Sample
    fun updateAt() {
        val a = atomicArrayOf("hello", "concurrent", "world")
        a.updateAt(1) { currentValue -> currentValue.uppercase() }
        assertPrints(a.toString(), "[hello, CONCURRENT, world]")

        // The index is out of array bounds
        assertFailsWith<IndexOutOfBoundsException> { a.updateAt(10) { it } }
    }

    @Sample
    fun updateAndFetchAt() {
        val a = atomicArrayOf("hello", "concurrent", "world")
        val updatedValue = a.updateAndFetchAt(1) { currentValue -> currentValue.uppercase() }
        assertPrints(updatedValue, "CONCURRENT")
        assertPrints(a.toString(), "[hello, CONCURRENT, world]")

        // The index is out of array bounds
        assertFailsWith<IndexOutOfBoundsException> { a.updateAndFetchAt(10) { it } }
    }

    @Sample
    fun fetchAndUpdateAt() {
        val a = atomicArrayOf("hello", "concurrent", "world")
        val oldValue = a.fetchAndUpdateAt(1) { currentValue -> currentValue.uppercase() }
        assertPrints(oldValue, "concurrent")
        assertPrints(a.toString(), "[hello, CONCURRENT, world]")

        // The index is out of array bounds
        assertFailsWith<IndexOutOfBoundsException> { a.fetchAndUpdateAt(10) { it } }
    }
}
