package kotlin.util

import kotlin.support.AbstractIterator

import java.util.*
import java.util.Iterator

/** Add iterated elements to given container */
fun <T,U: Collection<in T>> java.util.Iterator<T>.to(container: U) : U {
    while(hasNext())
        container.add(next())
    return container
}

/** Add iterated elements to java.util.ArrayList */
inline fun <T> java.util.Iterator<T>.toArrayList() = to(ArrayList<T>())

/** Add iterated elements to java.util.LinkedList */
inline fun <T> java.util.Iterator<T>.toLinkedList() = to(LinkedList<T>())

/** Add iterated elements to java.util.HashSet */
inline fun <T> java.util.Iterator<T>.toHashSet() = to(HashSet<T>())

/** Add iterated elements to java.util.LinkedHashSet */
inline fun <T> java.util.Iterator<T>.toLinkedHashSet() = to(LinkedHashSet<T>())

/** Add iterated elements to java.util.TreeSet */
inline fun <T> java.util.Iterator<T>.toTreeSet() = to(TreeSet<T>())

/** Filters the iterator for all elements of a certain kind */
inline fun <T, R: T> java.util.Iterator<T>.filterIs(): Iterator<R> {
    val iter = this
    return object : AbstractIterator<R>() {

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
}