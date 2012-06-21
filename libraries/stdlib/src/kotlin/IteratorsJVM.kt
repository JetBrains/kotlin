package kotlin

import kotlin.support.*
import java.util.Collections

/** Returns an iterator over elements that are instances of a given type *R* which is a subclass of *T* */
public inline fun <T, R: T> java.util.Iterator<T>.filterIsInstance(klass: Class<R>): java.util.Iterator<R> = FilterIsIterator<T,R>(this, klass)

private class FilterIsIterator<T, R :T>(val iterator : java.util.Iterator<T>, val klass: Class<R>) : AbstractIterator<R>() {
    override protected fun computeNext(): Unit {
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (klass.isInstance(next)) {
                setNext(next as R)
                return
            }
        }
        done()
    }
}

