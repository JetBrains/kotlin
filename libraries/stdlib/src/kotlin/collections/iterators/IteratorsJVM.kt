package kotlin

import kotlin.support.*

/** Returns an iterator over elements that are instances of a given type *R* which is a subclass of *T* */
deprecated("Use streams for lazy collection operations.")
public fun <T, R : T> Iterator<T>.filterIsInstance(klass: Class<R>): Iterator<R> = FilterIsIterator<T, R>(this, klass)

deprecated("Use streams for lazy collection operations.")
public class FilterIsIterator<T, R : T>(private val iterator: Iterator<T>, private val klass: Class<R>) :
        AbstractIterator<R>() {
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

