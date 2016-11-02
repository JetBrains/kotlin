package java.util

import java.lang.*
import java.util.*
import kotlin.comparisons.*

public object Collections {
    @Deprecated("Use collection.maxWith(comparator) instead.", ReplaceWith("col.maxWith(comp)"))
    public fun <T> max(col: Collection<T>, comp: Comparator<in T>): T = java.util.max(col, comp)

    @Deprecated("Use list.reverse() instead.", ReplaceWith("list.reverse()"))
    public fun <T> reverse(list: MutableList<T>): Unit {
        val size = list.size
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
