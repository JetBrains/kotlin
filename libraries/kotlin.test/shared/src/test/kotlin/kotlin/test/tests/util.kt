package kotlin.test.tests

import java.util.*

internal fun <T> listOf(vararg elements: T): List<T> {
    val result = ArrayList<T>(elements.size)
    for (e in elements) {
        result.add(e)
    }

    return result
}

internal fun <T> setOf(vararg elements: T): Set<T> {
    val result = HashSet<T>(elements.size)
    for (e in elements) {
        result.add(e)
    }

    return result
}

