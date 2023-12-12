/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

val sb = StringBuilder()

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resumeWith(result: Result<Any?>) { result.getOrThrow() }
}

suspend fun s1(): Unit = suspendCoroutineUninterceptedOrReturn { x ->
    sb.appendLine("s1")
    x.resume(Unit)
    COROUTINE_SUSPENDED
}

suspend fun s2() {
    sb.appendLine("s2")
    s1()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        s2()
    }

    assertEquals("""
        s2
        s1

    """.trimIndent(), sb.toString())
    return "OK"
}