/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

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

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

interface I {
    suspend fun foo(lambda: suspend (String) -> Unit)
    suspend fun bar(s: String)
}

fun create() = object: I {
    var lambda: suspend (String) -> Unit = {}

    override suspend fun foo(lambda: suspend (String) -> Unit) {
        this.lambda = lambda
    }

    override suspend fun bar(s: String) {
        lambda(s)
    }
}

fun box(): String {
    val sb = StringBuilder()

    builder {
        val z = create()
        z.foo { suspendHere(); sb.append(it) }
        z.bar("zzz")
    }
    assertEquals("zzz", sb.toString())
    return "OK"
}