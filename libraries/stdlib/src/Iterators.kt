package kotlin.util

import kotlin.support.AbstractIterator

import java.util.*
import java.util.Iterator

/** Filters the iterator for all elements of a certain kind */
inline fun <T, R: T> java.util.Iterator<T>.filterIs(): Iterator<R> = FilterIsIterator<T,R>(this)

private class FilterIsIterator<T, R :T>(val iter: java.util.Iterator<T>) : AbstractIterator<R>() {
    override fun computeNext(): R? {
        while (iter.hasNext()) {
            val next = iter.next()
            if (next is R) {
                return next as R
            }
        }
        done()
        return null
    }
}

/** Returns a new lazy iterator containing all elements in this iteration which match the given predicate */
/*
inline fun <T> java.util.Iterator<T>.filter(predicate: (T)-> Boolean) : java.util.Iterator<T> {
    return FilterIterator(this, predicate)
}

private class FilterIterator<T>(val iter: java.util.Iterator<T>, val predicate: (T)-> Boolean) : AbstractIterator<T>() {
    override fun computeNext(): T? {
        while (iter.hasNext()) {
            val next = iter.next()
            if (predicate(next)) {
                return next
            }
        }
        done()
        return null
    }
}
*/

/** Returns a new lazy iterator containing all elements in this iteration which do not match the given predicate */
    /*
    inline fun <T> java.util.Iterator<T>.filterNot(predicate: (T)-> Boolean) : java.util.Iterator<T> {
        return filter {
            !predicate(it)
        }
    }
    */

/** Returns a new lazy iterator containing all the non null elements in this iteration */
/*
inline fun <T> java.util.Iterator<T?>?.filterNotNull() : java.util.Iterator<T> = FilterNotNullIterator(this)

private class FilterNotNullIterator<T>(val iter: java.util.Iterator<T?>?) : AbstractIterator<T>() {
    override fun computeNext(): T? {
        if (iter != null) {
            while (iter.hasNext()) {
                val next = iter.next()
                if (next != null) {
                    return next
                }
            }
        }
        done()
        return null
    }
}
*/


/**
  * Returns a lazy iterator containing the result of transforming each item in the iteration to a one or more values which
  * are concatenated together into a single collection
  */
/*
inline fun <T, R> java.util.Iterator<T>.flatMap(transform: (T)-> java.util.Iterator<R>) : Collection<R> {
    return flatMapTo<>(ArrayList<R>(), transform)
}
*/
