/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

// This is almost a full copy of kotlin/compiler/tests-common/tests/org/jetbrains/kotlin/coroutineTestUtil.kt
// TODO: get it automatically as a dependency
fun createTextForHelpers(): String = """
package helpers
import kotlin.coroutines.*

fun <T> handleResultContinuation(x: (T) -> Unit): Continuation<T> = object: Continuation<T> {
    override val context = EmptyCoroutineContext
    
    override fun resumeWith(result: Result<T>) {
        x(result.getOrThrow())
    }
}


fun handleExceptionContinuation(x: (Throwable) -> Unit): Continuation<Any?> = object: Continuation<Any?> {
    override val context = EmptyCoroutineContext
    
    override fun resumeWith(result: Result<Any?>) {
        result.exceptionOrNull()?.let(x)
    }
}

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()

    override fun resumeWith(result: Result<Any?>) {
        result.getOrThrow()
    }
}

class StateMachineCheckerClass {
    private var counter = 0
    var finished = false

    var proceed: () -> Unit = {}
    
    fun reset() {
        counter = 0
        finished = false
        proceed = {}
    }

    suspend fun suspendHere() = suspendCoroutine<Unit> { c ->
        counter++
        proceed = { c.resume(Unit) }
    }

    fun check(numberOfSuspensions: Int, checkFinished: Boolean = true) {
        for (i in 1..numberOfSuspensions) {
            if (counter != i) error("Wrong state-machine generated: suspendHere called should be called exactly once in one state. Expected " + i + ", got " + counter)
            proceed()
        }
        if (counter != numberOfSuspensions)
            error("Wrong state-machine generated: suspendHere called should be called exactly once in one state. Expected " + numberOfSuspensions + ", got " + counter)
        if (finished) error("Wrong state-machine generated: it is finished early")
        proceed()
        if (checkFinished && !finished) error("Wrong state-machine generated: it is not finished yet")
    }
}
val StateMachineChecker = StateMachineCheckerClass()
object CheckStateMachineContinuation: Continuation<Unit>() {
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resumeWith(result: Result<Unit>) {
        result.getOrThrow()
        StateMachineChecker.proceed = {
            StateMachineChecker.finished = true
        }
    }
}
"""
