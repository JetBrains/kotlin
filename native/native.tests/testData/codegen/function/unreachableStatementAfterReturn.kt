/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.coroutines.*
import kotlin.test.*

fun test1(): Any? {
    return 1
    42
}

fun test2(): Int? {
    return 2
    42
}

fun test3(): Any {
    return 3
    42
}

fun test4(): Int {
    return 4
    42
}

suspend fun test5(): Any? {
    return 5
    42
}

suspend fun test6(): Int? {
    return 6
    42
}

suspend fun test7(): Any {
    return 7
    42
}

suspend fun test8(): Int {
    return 8
    42
}

@Suppress("UNCHECKED_CAST")
private fun <T> (suspend () -> T).runCoroutine() : T {
    var result : Any? = null
    startCoroutine(Continuation(EmptyCoroutineContext) { result = it.getOrThrow() })
    return result as T
}

fun box(): String {
    assertEquals(1, test1())
    assertEquals(2, test2())
    assertEquals(3, test3())
    assertEquals(4, test4())

    assertEquals(5, ::test5.runCoroutine())
    assertEquals(6, ::test6.runCoroutine())
    assertEquals(7, ::test7.runCoroutine())
    assertEquals(8, ::test8.runCoroutine())

    return "OK"
}

