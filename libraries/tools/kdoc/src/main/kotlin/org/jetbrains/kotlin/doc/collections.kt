package org.jetbrains.kotlin.doc

import java.util.HashMap

fun <A, K, V> List<A>.toHashMap(f: (A) -> Pair<K, V>): Map<K, V> {
    val r = HashMap<K, V>()
    for (item in this) {
        val entry = f(item)
        r.put(entry.first, entry.second)
    }
    return r
}


fun <K, V> List<V>.toHashMapMappingToKey(f: (V) -> K): Map<K, V> = toHashMap { v -> Pair(f(v), v) }
