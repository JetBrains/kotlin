/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
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

class Foo(val x: Int) {
    suspend fun bar(y: Int) = foo(y) + x
}

suspend fun foo(x: Int) = x

fun box(): String {
    val ref = Foo(42)::bar

    sb.appendLine((ref as Function2<Int, Continuation<Int>, Any?>)(117, EmptyContinuation))

    assertEquals("""
        159
        
    """.trimIndent(), sb.toString())
    return "OK"
}
