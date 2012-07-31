package java.util

import java.lang.*

//public object Collections {
//    library("collectionsMax")
//    public fun max<T>(col : Collection<T>, comp : Comparator<T>) : T = js.noImpl
//
//    // TODO should be immutable!
//    public val emptyList: List<Any> = ArrayList<Any>()
//    public val emptyMap: Map<Any, Any> = HashMap<Any,Any>()
//
//    public val <T> EMPTY_LIST: List<T>
//    get() = emptyList<T>()
//
//    public val <K,V> EMPTY_MAP: Map<K,V>
//    get() = emptyMap<K,V>()
//
//    public fun <T> emptyList(): List<T> = emptyList as List<T>
//    public fun <K,V> emptyMap(): Map<K,V> = emptyMap as Map<K,V>
//
//    public fun <in T> sort(list: List<T>): Unit {
//        throw UnsupportedOperationException()
//    }
//
//    public fun <in T> sort(list: List<T>, comparator: java.util.Comparator<T>): Unit {
//        throw UnsupportedOperationException()
//    }
//
//    public fun <T> reverse(list: List<T>): Unit {
//        val size = list.size()
//        for (i in 0.upto(size / 2)) {
//            val i2 = size - i
//            val tmp = list[i]
//            list[i] = list[i2]
//            list[i2] = tmp
//        }
//    }
//}
