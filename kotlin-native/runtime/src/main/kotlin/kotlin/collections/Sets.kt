/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.collections

// TODO: Add SingletonSet class
/**
 * Returns an immutable set containing only the specified object [element].
 */
public fun <T> setOf(element: T): Set<T> = hashSetOf(element)

@PublishedApi
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
internal actual inline fun <E> buildSetInternal(builderAction: MutableSet<E>.() -> Unit): Set<E> {
    return HashSet<E>().apply(builderAction).build()
}

@PublishedApi
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
internal actual inline fun <E> buildSetInternal(capacity: Int, builderAction: MutableSet<E>.() -> Unit): Set<E> {
    return HashSet<E>(capacity).apply(builderAction).build()
}
