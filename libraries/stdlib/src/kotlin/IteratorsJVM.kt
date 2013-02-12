package kotlin

import kotlin.support.*

/** Returns an iterator over elements that are instances of a given type *R* which is a subclass of *T* */
public inline fun <T, R: T> Iterator<T>.filterIsInstance(klass: Class<R>): Iterator<R> = FilterIsIterator<T,R>(this, klass)

private class FilterIsIterator<T, R :T>(val iterator : Iterator<T>, val klass: Class<R>) : AbstractIterator<R>() {
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

