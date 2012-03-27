package kotlin

import java.util.Collection
import java.util.ArrayList
import java.util.LinkedList
import java.util.HashSet
import java.util.LinkedHashSet
import java.util.TreeSet

/**
Helper to make jet.Iterator usable in for
*/
inline fun <T> Iterator<T>.iterator() = this

/**
Helper to make java.util.Iterator usable in for
*/
inline fun <T> java.util.Iterator<T>.iterator() = this

/**
Helper to make java.util.Enumeration usable in for
*/
fun <erased T> java.util.Enumeration<T>.iterator(): Iterator<T> = object: Iterator<T> {
  override val hasNext: Boolean
    get() = hasMoreElements()

  override fun next() = nextElement().sure()
}

/*
 * Extension functions on the standard Kotlin types to behave like the java.lang.* and java.util.* collections
 */

/**
Add iterated elements to given container
*/
fun <T,U: Collection<in T>> Iterator<T>.to(container: U) : U {
    while(hasNext)
        container.add(next())
    return container
}

/**
Add iterated elements to java.util.ArrayList
*/
inline fun <T> Iterator<T>.toArrayList() = to(ArrayList<T>())

/**
Add iterated elements to java.util.LinkedList
*/
inline fun <T> Iterator<T>.toLinkedList() = to(LinkedList<T>())

/**
Add iterated elements to java.util.HashSet
*/
inline fun <T> Iterator<T>.toHashSet() = to(HashSet<T>())

/**
Add iterated elements to java.util.LinkedHashSet
*/
inline fun <T> Iterator<T>.toLinkedHashSet() = to(LinkedHashSet<T>())

/**
Add iterated elements to java.util.TreeSet
*/
inline fun <T> Iterator<T>.toTreeSet() = to(TreeSet<T>())

/**
Run function f
*/
inline fun <T> run(f: () -> T) = f()
