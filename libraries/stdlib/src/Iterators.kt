package kotlin

import kotlin.support.AbstractIterator

import java.util.*
import java.util.Iterator

/** Filters the iterator for all elements of a certain sub class */
inline fun <T, R: T> java.util.Iterator<T>.filterIsInstance(klass: Class<R>): Iterator<R> = FilterIsIterator<T,R>(this, klass)

private class FilterIsIterator<T, R :T>(val iter: java.util.Iterator<T>, val klass: Class<R>) : AbstractIterator<R>() {
    override protected fun computeNext(): R? {
        while (iter.hasNext()) {
            val next = iter.next()
            if (klass.isInstance(next)) {
                return next as R
            }
        }
        done()
        return null
    }
}


// TODO when we're past the compiler error
// http://youtrack.jetbrains.com/issue/KT-1646
//
// we can use this code to replace the other
// filter / FilterIterator class as it reuses the
// core logic from AbstractIterator

/** Returns a new lazy iterator containing all elements in this iteration which match the given predicate */
inline fun <T> java.util.Iterator<T>.filter2(predicate: (T)-> Boolean) : java.util.Iterator<T> {
    return FilterIterator2(this, predicate)
}

private class FilterIterator2<T>(val iter: java.util.Iterator<T>, val fn: (T)-> Boolean) : AbstractIterator<T>() {
    override protected fun computeNext(): T? {
        /*

        TODO this block generates a compiler error:
        http://youtrack.jetbrains.com/issue/KT-1646

        while (iter.hasNext()) {
            val element = iter.next()
            if (fn(element)) {
                return element
            }
        }
        */
        done()
        return null
    }
}

/** Returns a new lazy iterator containing all elements in this iteration which do not match the given predicate */
inline fun <T> java.util.Iterator<T>.filterNot(predicate: (T)-> Boolean) : java.util.Iterator<T> {
    return filter {
        !predicate(it)
    }
}

/** Returns a new lazy iterator containing all the non null elements in this iteration */
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


/**
  * Returns a lazy iterator containing the result of transforming each item in the iteration to a one or more values which
  * are concatenated together into a single collection
  */
/*

  TODO implement a lazy flatMap

inline fun <T, R> java.util.Iterator<T>.flatMap(transform: (T)-> java.util.Iterator<R>) : Collection<R> {
    return flatMapTo<>(ArrayList<R>(), transform)
}
*/
