package java.util.Collections

import java.lang.*
import java.util.*

// TODO write tests for empty*

// TODO should be immutable!
public val emptyList: List<Any> = ArrayList<Any>()
public val emptyMap: Map<Any, Any> = HashMap<Any,Any>()

public val <T> EMPTY_LIST: List<T>
    get() = emptyList<T>()

public val <K,V> EMPTY_MAP: Map<K,V>
    get() = emptyMap<K,V>()

public fun <T> emptyList(): List<T> = emptyList as List<T>
public fun <K,V> emptyMap(): Map<K,V> = emptyMap as Map<K,V>

public fun <T> reverse(list: MutableList<T>): Unit {
    val size = list.size()
    for (i in 0..(size / 2) - 1) {
        val i2 = size - i - 1
        val tmp = list[i]
        list[i] = list[i2]
        list[i2] = tmp
    }
}