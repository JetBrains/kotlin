package java.util.Collections

import java.lang.*
import java.util.*

public fun <T> reverse(list: MutableList<T>): Unit {
    val size = list.size()
    for (i in 0..(size / 2) - 1) {
        val i2 = size - i - 1
        val tmp = list[i]
        list[i] = list[i2]
        list[i2] = tmp
    }
}