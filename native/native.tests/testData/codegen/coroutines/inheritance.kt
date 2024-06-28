/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.test.*

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resumeWith(result: Result<Any?>) { result.getOrThrow() }
}

class SuspendHere(): suspend () -> Int {
    override suspend fun invoke() : Int = suspendCoroutineUninterceptedOrReturn { x ->
        x.resume(42)
        COROUTINE_SUSPENDED
    }
}

// in old compiler versions all suspend functions were implementing this interface
// now they are not, but let's test it works correctly
class SuspendHereLegacy(): suspend () -> Int, SuspendFunction<Int> {
    override suspend fun invoke() : Int = suspendCoroutineUninterceptedOrReturn { x ->
        x.resume(43)
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = 0

    builder {
        result = SuspendHere()()
    }
    assertEquals(42, result)
    builder {
        result = SuspendHereLegacy()()
    }
    assertEquals(43, result)
    return "OK"
}