/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.coroutines.kt41394

import kotlin.test.*

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resumeWith(result: Result<Any?>) { result.getOrThrow() }
}

suspend fun suspendHere(): Int = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(42)
    COROUTINE_SUSPENDED
}

suspend fun foo(label: String): String {
    val x = suspendHere()
    return label + x.toString()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

@Test fun runTest() {
    var result = ""

    builder {
        result = foo("zzz")
    }

    assertEquals("zzz42", result)
}