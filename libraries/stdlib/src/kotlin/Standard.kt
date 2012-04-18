package kotlin

import java.util.Collection
import java.util.ArrayList
import java.util.LinkedList
import java.util.HashSet
import java.util.LinkedHashSet
import java.util.TreeSet
import java.util.SortedSet
import java.util.Comparator
import java.io.PrintWriter
import java.io.PrintStream

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
public fun <T,U: Collection<in T>> Iterator<T>.toCollection(container: U) : U {
    while(hasNext)
        container.add(next())
    return container
}

/**
Add iterated elements to java.util.ArrayList
*/
public inline fun <T> Iterator<T>.toArrayList() : ArrayList<T> = toCollection(ArrayList<T>())

/**
Add iterated elements to java.util.LinkedList
*/
public inline fun <T> Iterator<T>.toLinkedList() : LinkedList<T> = toCollection(LinkedList<T>())

/**
Add iterated elements to java.util.HashSet
*/
public inline fun <T> Iterator<T>.toHashSet() : HashSet<T> = toCollection(HashSet<T>())

/**
 * Add iterated elements to a [[LinkedHashSet]] to preserve insertion order
 */
public inline fun <T> Iterator<T>.toLinkedSet() : LinkedHashSet<T> = toCollection(LinkedHashSet<T>())

/**
 * Add iterated elements to [[SortedSet]] to ensure iteration is in the order of the default comparator
 * for the type
 */
public inline fun <T> Iterator<T>.toSortedSet() : SortedSet<T> = toCollection(TreeSet<T>())

/**
 * Add iterated elements to [[SortedSet]] with the given *comparator* to ensure iteration is in the order of the given comparator
 */
public inline fun <T> Iterator<T>.toSortedSet(comparator: Comparator<T>) : SortedSet<T> = toCollection(TreeSet<T>(comparator))


/**
 * Creates a tuple of type [[#(A,B)]] from this and *that* which can be useful for creating [[Map]] literals
 * with less noise, for example

 * @includeFunctionBody ../../test/MapTest.kt createUsingTo
 */
public inline fun <A,B> A.to(that: B): #(A, B) = #(this, that)

/**
Run function f
*/
public inline fun <T> run(f: () -> T) : T = f()

/**
 * A helper method for creating a [[Runnable]] from a function
 */
public inline fun runnable(action: ()-> Unit): Runnable {
    return object: Runnable {
        public override fun run() {
            action()
        }
    }
}

/**
 * Allows a stack trace to be printed from Kotlin's [[Throwable]]
 */
public inline fun Throwable.printStackTrace(writer: PrintWriter): Unit {
    val jlt = this as java.lang.Throwable
    jlt.printStackTrace(writer)
}

/**
 * Allows a stack trace to be printed from Kotlin's [[Throwable]]
 */
public inline fun Throwable.printStackTrace(stream: PrintStream): Unit {
    val jlt = this as java.lang.Throwable
    jlt.printStackTrace(stream)
}
