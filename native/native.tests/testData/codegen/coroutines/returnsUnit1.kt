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

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

inline suspend fun inline_s2(x: Int): Unit {
    sb.appendLine(x)
    s1()
}

suspend fun s3(x: Int) {
    inline_s2(x)
}

fun box(): String {
    var result = 0

    builder {
        s3(117)
    }

    sb.appendLine(result)

    assertEquals("""
        117
        s1
        0

    """.trimIndent(), sb.toString())
    return "OK"
}