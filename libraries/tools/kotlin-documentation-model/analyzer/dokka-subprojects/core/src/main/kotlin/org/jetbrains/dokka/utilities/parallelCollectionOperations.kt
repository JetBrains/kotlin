/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.utilities

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.dokka.InternalDokkaApi

@InternalDokkaApi
public suspend inline fun <A, B> Iterable<A>.parallelMap(crossinline f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll()
}

@InternalDokkaApi
public suspend inline fun <A, B> Iterable<A>.parallelMapNotNull(crossinline f: suspend (A) -> B?): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll().filterNotNull()
}

@InternalDokkaApi
public suspend inline fun <A> Iterable<A>.parallelForEach(crossinline f: suspend (A) -> Unit): Unit = coroutineScope {
    forEach { launch { f(it) } }
}
