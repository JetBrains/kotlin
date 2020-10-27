/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.coroutines.controlFlow_finally7

import kotlin.test.*

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resumeWith(result: Result<Any?>) { result.getOrThrow() }
}

suspend fun s1(): Int = suspendCoroutineUninterceptedOrReturn { x ->
    println("s1")
    x.resumeWithException(Error())
    COROUTINE_SUSPENDED
}

suspend fun s2(): Int = suspendCoroutineUninterceptedOrReturn { x ->
    println("s2")
    x.resume(42)
    COROUTINE_SUSPENDED
}

fun f1(): Int {
    println("f1")
    return 117
}

fun f2(): Int {
    println("f2")
    return 1
}

fun f3(x: Int, y: Int): Int {
    println("f3")
    return x + y
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

@Test fun runTest() {
    var result = 0

    builder {
        try {
            try {
                result = s1()
            } catch (t: Throwable) {
                result = f2()
            } finally {
                println("finally1")
                result = s2()
            }
        } finally {
            println("finally2")
        }
    }

    println(result)
}