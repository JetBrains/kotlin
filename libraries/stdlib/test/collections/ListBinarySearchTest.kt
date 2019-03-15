/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.collections.binarySearch

import kotlin.test.*

class ListBinarySearchTest {

    val values = listOf(1, 3, 7, 10, 12, 15, 22, 45)

    fun notFound(index: Int) = -(index + 1)

    private val comparator = compareBy<IncomparableDataItem<Int>?> { it?.value }

    @Test
    fun binarySearchByElement() {
        val list = values
        list.forEachIndexed { index, item ->
            assertEquals(index, list.binarySearch(item))
            assertEquals(notFound(index), list.binarySearch(item.pred()))
            assertEquals(notFound(index + 1), list.binarySearch(item.succ()))

            if (index > 0) {
                index.let { from -> assertEquals(notFound(from), list.binarySearch(list.first(), fromIndex = from)) }
                (list.size - index).let { to -> assertEquals(notFound(to), list.binarySearch(list.last(), toIndex = to)) }
            }
        }
    }

    @Test
    fun binarySearchByElementNullable() {
        val list = listOf(null) + values
        list.forEachIndexed { index, item ->
            assertEquals(index, list.binarySearch(item))

            if (index > 0) {
                index.let { from -> assertEquals(notFound(from), list.binarySearch(list.first(), fromIndex = from)) }
                (list.size - index).let { to -> assertEquals(notFound(to), list.binarySearch(list.last(), toIndex = to)) }
            }
        }
    }

    @Test
    fun binarySearchWithComparator() {
        val list = values.map { IncomparableDataItem(it) }

        list.forEachIndexed { index, item ->
            assertEquals(index, list.binarySearch(item, comparator))
            assertEquals(notFound(index), list.binarySearch(item.pred(), comparator))
            assertEquals(notFound(index + 1), list.binarySearch(item.succ(), comparator))

            if (index > 0) {
                index.let { from -> assertEquals(notFound(from), list.binarySearch(list.first(), comparator, fromIndex = from)) }
                (list.size - index).let { to -> assertEquals(notFound(to), list.binarySearch(list.last(), comparator, toIndex = to)) }
            }
        }
    }

    @Test
    fun binarySearchByKey() {
        val list = values.map { IncomparableDataItem(it) }

        list.forEachIndexed { index, item ->
            assertEquals(index, list.binarySearchBy(item.value) { it.value })
            assertEquals(notFound(index), list.binarySearchBy(item.value.pred()) { it.value })
            assertEquals(notFound(index + 1), list.binarySearchBy(item.value.succ()) { it.value })

            if (index > 0) {
                index.let { from -> assertEquals(notFound(from), list.binarySearchBy(list.first().value, fromIndex = from) { it.value }) }
                (list.size - index).let { to -> assertEquals(notFound(to), list.binarySearchBy(list.last().value, toIndex = to) { it.value }) }
            }
        }
    }


    @Test
    fun binarySearchByKeyWithComparator() {
        val list = values.map { IncomparableDataItem(IncomparableDataItem(it)) }

        list.forEachIndexed { index, item ->
            assertEquals(index, list.binarySearch { comparator.compare(it.value, item.value) })
            assertEquals(notFound(index), list.binarySearch { comparator.compare(it.value, item.value.pred()) })
            assertEquals(notFound(index + 1), list.binarySearch { comparator.compare(it.value, item.value.succ()) })

            if (index > 0) {
                index.let { from ->
                    assertEquals(notFound(from), list.binarySearch(fromIndex = from) { comparator.compare(it.value, list.first().value) })
                }
                (list.size - index).let { to ->
                    assertEquals(notFound(to), list.binarySearch(toIndex = to) { comparator.compare(it.value, list.last().value) })
                }
            }
        }
    }

    @Test
    fun binarySearchByMultipleKeys() {
        val list = values.flatMap { v1 -> values.map { v2 -> Pair(v1, v2) } }

        list.forEachIndexed { index, item ->
            assertEquals(index, list.binarySearch { compareValuesBy(it, item, { it.first }, { it.second }) })
        }
    }
}


private data class IncomparableDataItem<T>(public val value: T)
private fun IncomparableDataItem<Int>.pred(): IncomparableDataItem<Int> = IncomparableDataItem(value - 1)
private fun IncomparableDataItem<Int>.succ(): IncomparableDataItem<Int> = IncomparableDataItem(value + 1)
private fun Int.pred() = dec()
private fun Int.succ() = inc()


