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
public inline fun <T> Iterator<T>.iterator() : Iterator<T> = this

/**
Helper to make java.util.Iterator usable in for
*/
public inline fun <T> java.util.Iterator<T>.iterator() : java.util.Iterator<T> = this

/**
Helper to make java.util.Enumeration usable in for
*/
public fun <erased T> java.util.Enumeration<T>.iterator(): Iterator<T> = object: Iterator<T> {
    override val hasNext: Boolean
    get() = hasMoreElements()

    public override fun next() : T = nextElement().sure()
}

/*
 * Extension functions on the standard Kotlin types to behave like the java.lang.* and java.util.* collections
 */

/**
Add iterated elements to given container
*/
public fun <T,U: Collection<in T>> Iterator<T>.to(container: U) : U {
    while(hasNext)
        container.add(next())
    return container
}

/**
Add iterated elements to java.util.ArrayList
*/
public inline fun <T> Iterator<T>.toArrayList() : ArrayList<T> = to(ArrayList<T>())

/**
Add iterated elements to java.util.LinkedList
*/
public inline fun <T> Iterator<T>.toLinkedList() : LinkedList<T> = to(LinkedList<T>())

/**
Add iterated elements to java.util.HashSet
*/
public inline fun <T> Iterator<T>.toHashSet() : HashSet<T> = to(HashSet<T>())

/**
Add iterated elements to java.util.LinkedHashSet
*/
public inline fun <T> Iterator<T>.toLinkedHashSet() : LinkedHashSet<T> = to(LinkedHashSet<T>())

/**
Add iterated elements to java.util.TreeSet
*/
public inline fun <T> Iterator<T>.toTreeSet() : TreeSet<T> = to(TreeSet<T>())

/**
Run function f
*/
public inline fun <T> run(f: () -> T) : T = f()
