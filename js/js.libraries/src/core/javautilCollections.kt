package java.util

import java.lang.*
import java.util.*

public object Collections {
    @Deprecated("Use maxBy instead")
    public fun max<T>(col: Collection<T>, comp: Comparator<in T>): T = java.util.max(col, comp)

    public fun <T> sort(list: MutableList<T>): Unit = java.util.sort(list)

    public fun <T> sort(list: MutableList<T>, comparator: java.util.Comparator<in T>): Unit = java.util.sort(list, comparator)

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
internal fun max<T>(col: Collection<T>, comp: Comparator<in T>): T = noImpl

@library("collectionsSort")
internal fun <T> sort(list: MutableList<T>): Unit = noImpl

@library("collectionsSort")
internal fun <T> sort(list: MutableList<T>, comparator: java.util.Comparator<in T>): Unit = noImpl
