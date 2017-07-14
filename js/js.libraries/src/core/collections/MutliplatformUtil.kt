package kotlin.collections

/**
 * Created by user on 7/13/17.
 */

internal fun <T> Array<T>.resetAt(index: Int) {
    this.unsafeCast<Array<Any?>>()[index] = null
}

internal fun <T> Array<T>.resetRange(fromIndex: Int, toIndex: Int) {
    val arr = this.unsafeCast<Array<Any?>>()
    for (i in fromIndex until toIndex) {
        arr[i] = null
    }
}

fun Int.highestOneBit() : Int {
    var index = 31

    while (index >= 0) {
        var mask = (1 shl index)
        if ((mask and this) != 0) {
            return mask
        }
        index--
    }
    return 0
}

fun Int.numberOfLeadingZeros() : Int {
    var index = 31

    while (index >= 0) {
        var mask = (1 shl index)
        if ((mask and this) != 0) {
            return 31 - index
        }
        index--
    }
    return 0
}

internal fun  <T> Array<T>.copyOfUninitializedElements(newSize: Int): Array<T> {
    return this.copyOf(newSize).unsafeCast<Array<T>>()
}

internal fun  IntArray.copyOfUninitializedElements(newSize: Int): IntArray {
    return this.copyOf(newSize)
}

internal fun IntArray.fill(value: Int, fromIndex: Int, toIndex: Int) {
    for (i in fromIndex until toIndex) {
        this[i] = value
    }
}

internal fun  <T> arrayOfUninitializedElements(capacity: Int): Array<T> {
    return arrayOfNulls<Any>(capacity).unsafeCast<Array<T>>()
}


/**
 * Constructs the specialized implementation of [HashMap] with [String] keys, which stores the keys as properties of
 * JS object without hashing them.
 */
internal fun <V> stringMapOf(vararg pairs: Pair<String, V>): HashMap<String, V> {
    return HashMap<String, V>().apply { putAll(pairs) }
}

/**
 * Creates a new instance of the specialized implementation of [HashSet] with the specified [String] elements,
 * which elements the keys as properties of JS object without hashing them.
 */
internal fun stringSetOf(vararg elements: String): HashSet<String> {
    return HashSet(stringMapOf<Any>()).apply { addAll(elements) }
}

/**
 * Constructs the specialized implementation of [LinkedHashMap] with [String] keys, which stores the keys as properties of
 * JS object without hashing them.
 */
public fun <V> linkedStringMapOf(vararg pairs: Pair<String, V>): LinkedHashMap<String, V> {
    return LinkedHashMap<String, V>().apply { putAll(pairs) }
}

/**
 * Creates a new instance of the specialized implementation of [LinkedHashSet] with the specified [String] elements,
 * which elements the keys as properties of JS object without hashing them.
 */
public fun linkedStringSetOf(vararg elements: String): LinkedHashSet<String> {
    return LinkedHashSet<String>().apply { addAll(elements) }
}

internal fun Any?.hashCode(): Int = this?.hashCode() ?: 0