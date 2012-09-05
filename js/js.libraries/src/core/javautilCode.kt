package java.util

import java.lang.*

public object Collections {
    library("collectionsMax")
    public fun max<T>(col : Collection<T>, comp : Comparator<T>) : T = js.noImpl

    // TODO should be immutable!
    library
    public val emptyList: List<Any> = ArrayList<Any>()
    library
    public val emptyMap: Map<Any, Any> = HashMap<Any,Any>()

    library
    public val <T> EMPTY_LIST: List<T>
    get() = emptyList<T>()

    library
    public val <K,V> EMPTY_MAP: Map<K,V>
    get() = emptyMap<K,V>()

    library
    public fun <T> emptyList(): List<T> = emptyList as List<T>
    library
    public fun <K,V> emptyMap(): Map<K,V> = emptyMap as Map<K,V>

    library
    public fun <in T> sort(list: List<T>): Unit {
        throw UnsupportedOperationException()
    }

    library("sortWithComp")
    public fun <in T> sort(list: List<T>, comparator: java.util.Comparator<T>): Unit {
        throw UnsupportedOperationException()
    }

    library
    public fun <T> reverse(list: MutableList<T>): Unit {
        val size = list.size()
        for (i in 0.rangeTo(size / 2)) {
            val i2 = size - i
            val tmp = list[i]
            list[i] = list[i2]
            list[i2] = tmp
        }
    }
}
