/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package codegen.function.unreachable_statement_after_return
import kotlin.coroutines.*

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

fun main() {
    println(test1())
    println(test2())
    println(test3())
    println(test4())

    println(::test5.runCoroutine())
    println(::test6.runCoroutine())
    println(::test7.runCoroutine())
    println(::test8.runCoroutine())
}

