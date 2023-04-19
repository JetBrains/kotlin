/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * Replaces each element in the list with a result of a transformation specified.
 */
internal fun <T> MutableList<T>.replaceAll(transformation: (T) -> T) {
    val it = listIterator()
    while (it.hasNext()) {
        val element = it.next()
        it.set(transformation(element))
    }
}