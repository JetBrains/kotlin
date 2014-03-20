package kotlin

import java.util.*

public inline fun <T> Iterable<T>.toString(): String {
    return makeString(", ", "[", "]")
}

/**
 * Returns a new Map containing the results of applying the given *transform* function to each [[Map.Entry]] in this [[Map]]
 *
 * @includeFunctionBody ../../test/MapTest.kt mapValues
 */
public inline fun <K,V,R> MutableMap<K,V>.mapValues(transform : (Map.Entry<K,V>) -> R): Map<K,R> {
    return mapValuesTo(HashMap<K,R>(), transform)
}

