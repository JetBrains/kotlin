package kotlin

import java.util.ArrayList
import java.util.HashSet
import java.util.LinkedList

/**
Helper to make java.util.Enumeration usable in for
*/
public fun <T> java.util.Enumeration<T>.iterator(): Iterator<T> = object: Iterator<T> {
    override fun hasNext(): Boolean = hasMoreElements()

    public override fun next() : T = nextElement()
}

/*
 * Extension functions on the standard Kotlin types to behave like the java.lang.* and java.util.* collections
 */

/**
Add iterated elements to given container
*/
/*
public fun <T,U: Collection<in T>> Iterator<T>.toCollection(container: U) : U {
    while(hasNext())
        container.add(next())
    return container
}
*/

/**
Add iterated elements to java.util.ArrayList
*/
public inline fun <T> Iterator<T>.toArrayList() : ArrayList<T> = toCollection(ArrayList<T>())

/**
Add iterated elements to java.util.HashSet
*/
public inline fun <T> Iterator<T>.toHashSet() : HashSet<T> = toCollection(HashSet<T>())


/**
 * Creates a tuple of type [[Pair<A,B>]] from this and *that* which can be useful for creating [[Map]] literals
 * with less noise, for example

 * @includeFunctionBody ../../test/MapTest.kt createUsingTo
 */
public inline fun <A,B> A.to(that: B): Pair<A, B> = Pair(this, that)

/**
Run function f
*/
public inline fun <T> run(f: () -> T) : T = f()
