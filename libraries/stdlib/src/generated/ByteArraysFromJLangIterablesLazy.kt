// NOTE this file is auto-generated from src/kotlin/JLangIterablesLazy.kt
package kotlin

import kotlin.util.*

import java.util.ArrayList
import java.util.Collection
import java.util.List

//
// This file contains methods which could have a lazy implementation for things like
// Iterator<Byte> or java.util.Iterator<Byte>
//
// See [[GenerateStandardLib.kt]] for more details
//

/**
 * Returns a list containing all elements which match the given *predicate*
 *
 * @includeFunctionBody ../../test/CollectionTest.kt filter
 */
public inline fun ByteArray.filter(predicate: (Byte) -> Boolean) : List<Byte> = filterTo(ArrayList<Byte>(), predicate)

/**
 * Returns a list containing all elements which do not match the given predicate
 *
 * @includeFunctionBody ../../test/CollectionTest.kt filterNot
 */
public inline fun ByteArray.filterNot(predicate: (Byte)-> Boolean) : List<Byte> = filterNotTo(ArrayList<Byte>(), predicate)

/**
 * Returns a list containing all the non-*null* elements
 *
 * @includeFunctionBody ../../test/CollectionTest.kt filterNotNull
 */
public inline fun ByteArray?.filterNotNull() : List<Byte> = filterNotNullTo<ArrayList<Byte>>(java.util.ArrayList<Byte>())

/**
 * Returns the result of transforming each element to one or more values which are concatenated together into a single collection
 *
 * @includeFunctionBody ../../test/CollectionTest.kt flatMap
 */
public inline fun <R> ByteArray.flatMap(transform: (Byte)-> Collection<R>) : Collection<R> = flatMapTo(ArrayList<R>(), transform)

/**
 * Creates a copy of this collection as a [[List]] with the element added at the end
 *
 * @includeFunctionBody ../../test/CollectionTest.kt plus
 */
public inline fun  ByteArray.plus(element: Byte): List<Byte> {
    val list = toCollection(ArrayList<Byte>())
    list.add(element)
    return list
}


/**
 * Creates a copy of this collection as a [[List]] with all the elements added at the end
 *
 * @includeFunctionBody ../../test/CollectionTest.kt plusCollection
 */
public inline fun  ByteArray.plus(elements: ByteArray): List<Byte> {
    val list = toCollection(ArrayList<Byte>())
    list.addAll(elements.toCollection())
    return list
}

/**
 * Returns a list containing all the non-*null* elements, throwing an [[IllegalArgumentException]] if there are any null elements
 *
 * @includeFunctionBody ../../test/CollectionTest.kt requireNoNulls
 */
public inline fun  ByteArray?.requireNoNulls() : List<Byte> {
    val list = ArrayList<Byte>()
    for (element in this) {
        if (element == null) {
            throw IllegalArgumentException("null element found in $this")
        } else {
            list.add(element)
        }
    }
    return list
}

/**
 * Returns a list containing the first *n* elements
 *
 * @includeFunctionBody ../../test/CollectionTest.kt take
 */
public inline fun ByteArray.take(n: Int): List<Byte> {
    fun countTo(n: Int): (Byte) -> Boolean {
      var count = 0
      return { ++count; count <= n }
    }
    return takeWhile(countTo(n))
}

/**
 * Returns a list containing the first elements that satisfy the given *predicate*
 *
 * @includeFunctionBody ../../test/CollectionTest.kt takeWhile
 */
public inline fun ByteArray.takeWhile(predicate: (Byte) -> Boolean): List<Byte> = takeWhileTo(ArrayList<Byte>(), predicate)
