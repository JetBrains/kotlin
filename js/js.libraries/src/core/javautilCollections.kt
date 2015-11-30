package java.util

import java.lang.*
import java.util.*

public object Collections {
    @Deprecated("Use collection.maxWith(comparator) instead.", ReplaceWith("col.maxWith(comp)"))
    public fun <T> max(col: Collection<T>, comp: Comparator<in T>): T = java.util.max(col, comp)

    @Deprecated("Use list.sort() instead.", ReplaceWith("list.sort()"))
    public fun <T: Comparable<T>> sort(list: MutableList<T>): Unit = java.util.sort(list, naturalOrder())

    @Deprecated("Use list.sortWith(comparator) instead.", ReplaceWith("list.sortWith(comparator)"))
    public fun <T> sort(list: MutableList<T>, comparator: java.util.Comparator<in T>): Unit = java.util.sort(list, comparator)

    @Deprecated("Use list.reverse() instead.", ReplaceWith("list.reverse()"))
    public fun <T> reverse(list: MutableList<T>): Unit {
        val size = list.size()
        for (i in 0..(size / 2) - 1) {
            val i2 = size - i - 1
            val tmp = list[i]
            list[i] = list[i2]
            list[i2] = tmp
        }
    }
}

@library("collectionsMax")
private fun <T> max(col: Collection<T>, comp: Comparator<in T>): T = noImpl

@library("collectionsSort")
private fun <T> sort(list: MutableList<T>, comparator: Comparator<in T>): Unit = noImpl
