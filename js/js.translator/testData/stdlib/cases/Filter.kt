package kotlin

import java.util.*
import java.lang.Iterable

/*
Filters given iterator
*/
inline fun <T> java.util.Iterator<T>.filter(f: (T) -> Boolean): java.util.Iterator<T> = FilterIterator<T>(this, f)

/*
Adds filtered elements in to given container
*/
inline fun <T, U : Collection<in T>> java.lang.Iterable<T>.filterTo(var container: U, filter: (T) -> Boolean): U {
    for (element in this) {
        if (filter(element))
            container.add(element)
    }
    return container
}

/*
Create iterator filtering given java.lang.Iterable
*/
/*
inline fun <T> java.lang.Iterable<T>.filter(f: (T)->Boolean) : java.util.Iterator<T> = (iterator() as java.util.Iterator<T>).filter(f)
*/

private class FilterIterator<T>(val original: java.util.Iterator<T>, val filter: (T) -> Boolean) : java.util.Iterator<T> {
    var state = 0
    var nextElement: T? = null

    override fun hasNext(): Boolean =
            when(state) {
                1 -> true  // checked and next present
                2 -> false // checked and next not present
                else -> {
                    while (original.hasNext()) {
                        val candidate = original.next()
                        if ((filter)(candidate)) {
                            nextElement = candidate
                            state = 1
                            true
                        }
                    }
                    state = 2
                    false
                }
            }

    override fun next(): T =
            if (state != 1)
                throw java.util.NoSuchElementException()
            else {
                val res = nextElement as T
                nextElement = null
                state = 0
                res
            }
}
