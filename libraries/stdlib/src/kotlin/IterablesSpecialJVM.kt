package kotlin

import java.util.AbstractList
import java.util.Comparator

// TODO this function is here as it breaks the JS compiler; lets move back to JLangIterablesSpecial when it works again :)

/**
 * Copies all elements into a [[List]] and sorts it by value of compare_function(element)
 *
 * E.g. arrayList("two" to 2, "one" to 1).sortBy({it._2}) returns list sorted by second element of tuple
 *
 * @includeFunctionBody ../../test/CollectionTest.kt sortBy
 */
public inline fun <in T, R: Comparable<in R>> Iterable<T>.sortBy(f: (T) -> R): List<T> {
    val sortedList = this.toList()
    val sortBy: Comparator<T> = comparator<T> {(x: T, y: T) ->
        val xr = f(x)
        val yr = f(y)
        xr.compareTo(yr)
    }
    java.util.Collections.sort(sortedList, sortBy)
    return sortedList
}
