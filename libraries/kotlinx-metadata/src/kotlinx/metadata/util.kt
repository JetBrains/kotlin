/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlinx.metadata

/**
 * Assigns the contents of [collection] into this list. After this function completes, the list and [collection]
 * will have the same size and will consist of the same elements in the same order.
 *
 * Useful for rewriting contents of any repeated Km* element represented by a [MutableList], e.g.:
 *
 *     val klass: KmClass
 *     klass.functions.assignFrom(...)
 *
 */
fun <T> MutableList<T>.assignFrom(collection: Collection<T>) {
    clear()
    addAll(collection)
}
