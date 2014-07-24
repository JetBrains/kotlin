package kotlin

import java.util.Enumeration

/**
Helper to make java.util.Enumeration usable in for
 */
public fun <T> Enumeration<T>.iterator(): Iterator<T> = object: Iterator<T> {
    override fun hasNext(): Boolean = hasMoreElements()

    public override fun next(): T = nextElement()
}

/**
 * Returns the given iterator itself. This allows to use an instance of iterator in a ranged for-loop
 */
public fun <T> Iterator<T>.iterator(): Iterator<T> = this
