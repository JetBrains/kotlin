// NOTE this file is auto-generated from src/JavaIterablesLazy.kt
package kotlin

import kotlin.util.*

import java.util.*

//
// This file contains methods which could have a lazy implementation for things like
// Iterator<T> or java.util.Iterator<T>
//
// See [[GenerateStandardLib.kt]] for more details
//

/** Returns a new List containing all elements in this collection which match the given predicate */
inline fun <T> Array<T>.filter(predicate: (T)-> Boolean) : Collection<T> = filterTo(java.util.ArrayList<T>(), predicate)

/** Returns a List containing all the non null elements in this collection */
inline fun <T> Array<T?>?.filterNotNull() : Collection<T> = filterNotNullTo<T, java.util.ArrayList<T>>(java.util.ArrayList<T>())

/** Returns a new collection containing all elements in this collection which do not match the given predicate */
inline fun <T> Array<T>.filterNot(predicate: (T)-> Boolean) : Collection<T> =  filterNotTo(ArrayList<T>(), predicate)

/**
  * Returns the result of transforming each item in the collection to a one or more values which
  * are concatenated together into a single collection
  */
inline fun <T, R> Array<T>.flatMap(transform: (T)-> Collection<R>) : Collection<R> {
    return flatMapTo(ArrayList<R>(), transform)
}

