// NATIVE_STANDALONE
package java.util

interface Map<K, V> {
    fun getOrDefault(key: K, default: V): V
}

class EmptyIntMap : Map<Int, Int> {
    override fun getOrDefault(key: Int, default: Int) = default
}

fun box() = "OK"