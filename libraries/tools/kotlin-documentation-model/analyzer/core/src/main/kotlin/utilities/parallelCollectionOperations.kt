package org.jetbrains.dokka.utilities

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

suspend inline fun <A, B> Iterable<A>.parallelMap(crossinline f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll()
}

suspend inline fun <A, B> Iterable<A>.parallelMapNotNull(crossinline f: suspend (A) -> B?): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll().filterNotNull()
}

suspend inline fun <A> Iterable<A>.parallelForEach(crossinline f: suspend (A) -> Unit): Unit = coroutineScope {
    map { async { f(it) } }.awaitAll()
}
