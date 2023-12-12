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

suspend fun s1(): Int = suspendCoroutineUninterceptedOrReturn { x ->
    sb.appendLine("s1")
    x.resume(42)
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun f1(): Int {
    sb.appendLine("f1")
    return 117
}

fun f2(): Int {
    sb.appendLine("f2")
    return 1
}

inline suspend fun inline_s2(): Int {
    var x = 0
    if (f1() > 0)
        x = s1()
    else x = f2()
    return x
}

fun box(): String {
    var result = 0

    builder {
        result = inline_s2()
    }

    sb.appendLine(result)

    assertEquals("""
        f1
        s1
        42

    """.trimIndent(), sb.toString())
    return "OK"
}
