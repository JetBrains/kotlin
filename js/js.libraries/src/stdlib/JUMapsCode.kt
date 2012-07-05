package kotlin

import java.util.Map
import java.util.HashMap

/** Provides [] access to maps */
public fun <K, V> Map<K, V>.set(key : K, value : V): Unit {
    this.put(key, value)
}

/**
 * Returns a new [[HashMap]] populated with the given tuple values where the first value in each tuple
 * is the key and the second value is the value
 *
 * @includeFunctionBody ../../test/MapTest.kt createUsingTuples
 */
public inline fun <K,V> hashMap(vararg values: #(K,V)): HashMap<K,V> {
    val answer = HashMap<K,V>()
    /**
        TODO replace by this simpler call when we can pass vararg values into other methods
        answer.putAll(values)
    */
    for (v in values) {
        answer.put(v._1, v._2)
    }
    return answer
}