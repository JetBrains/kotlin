/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.test.*

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resumeWith(result: Result<Any?>) { result.getOrThrow() }
}

suspend fun suspendForever(): Int = suspendCoroutineUninterceptedOrReturn {
    COROUTINE_SUSPENDED
}
// CHECK-LABEL: define %struct.ObjHeader* @"kfun:$fooCOROUTINE

// CHECK-NOT: ; Function Attrs: noreturn
// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#foo(){}kotlin.Nothing"
suspend fun foo(): Nothing {
    suspendForever()
    throw Error()
}

suspend fun bar() {
    foo()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun main() {
    builder {
        bar()
    }
    println("OK")
}

