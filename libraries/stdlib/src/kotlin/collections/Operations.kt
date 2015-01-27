package kotlin

import java.util.ArrayList
import kotlin.platform.platformName

/**
 * Returns a single list of all elements from all collections in original collection
 */
public fun <T> Iterable<Iterable<T>>.flatten(): List<T> {
    val result = ArrayList<T>()
    for (element in this) {
        result.addAll(element)
    }
    return result
}

/**
 * Returns a stream of all elements from all streams in original stream
 */
public fun <T> Stream<Stream<T>>.flatten(): Stream<T> {
    return FlatteningStream(this, { it })
}

/**
 * Returns a single list of all elements from all collections in original collection
 */
public fun <T> Array<Array<out T>>.flatten(): List<T> {
    val result = ArrayList<T>(sumBy { it.size() })
    for (element in this) {
        result.addAll(element)
    }
    return result
}

