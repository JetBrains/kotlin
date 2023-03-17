/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * Returns a list containing all elements that are not `null`.
 */
public actual inline fun <reified T : Any> VArray<T?>.filterNotNull(): List<T> {
    return filterNotNullTo(ArrayList<T>())
}

/**
 * Appends all elements that are not `null` to the given [destination].
 */
public inline fun <C : MutableCollection<in T>, reified T : Any> VArray<T?>.filterNotNullTo(destination: C): C {
    for (element in this) if (element != null) destination.add(element)
    return destination
}
