/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.coroutines.experimental.intrinsics

import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.processBareContinuationResume
import konan.internal.*

/**
 * Obtains the current continuation instance inside suspend functions and either suspend
 * currently running coroutine or return result immediately without suspension.
 *
 * If the [block] returns the special [COROUTINE_SUSPENDED] value, it means that suspend function did suspend the execution and will
 * not return any result immediately. In this case, the [Continuation] provided to the [block] shall be invoked at some moment in the
 * future when the result becomes available to resume the computation.
 *
 * Otherwise, the return value of the [block] must have a type assignable to [T] and represents the result of this suspend function.
 * It means that the execution was not suspended and the [Continuation] provided to the [block] shall not be invoked.
 * As the result type of the [block] is declared as `Any?` and cannot be correctly type-checked,
 * its proper return type remains on the conscience of the suspend function's author.
 *
 * Note that it is not recommended to call either [Continuation.resume] nor [Continuation.resumeWithException] functions synchronously
 * in the same stackframe where suspension function is run. Use [suspendCoroutine] as a safer way to obtain current
 * continuation instance.
 */
@kotlin.internal.InlineOnly
public inline suspend fun <T> suspendCoroutineOrReturn(crossinline block: (Continuation<T>) -> Any?): T =
        returnIfSuspended<T>(block(normalizeContinuation<T>(getContinuation<T>())))

/**
 * This value is used as a return value of [suspendCoroutineOrReturn] `block` argument to state that
 * the execution was suspended and will not return any result immediately.
 */
public val COROUTINE_SUSPENDED: Any = Any()

/**
 * Creates a coroutine without receiver and with result type [T].
 * This function creates a new, fresh instance of suspendable computation every time it is invoked.
 *
 * To start executing the created coroutine, invoke `resume(Unit)` on the returned [Continuation] instance.
 * The [completion] continuation is invoked when coroutine completes with result or exception.
 *
 * This function is _unchecked_. Repeated invocation of any resume function on the resulting continuation corrupts the
 * state machine of the coroutine and may result in arbitrary behaviour or exception.
 */
public fun <T> (suspend () -> T).createCoroutineUnchecked(
        completion: Continuation<T>
): Continuation<Unit> =
        if (this !is CoroutineImpl)
            buildContinuationByInvokeCall(completion) {
                (this as Function1<Continuation<T>, Any?>).invoke(completion)
            }
        else
            (this.create(completion) as CoroutineImpl).facade

/**
 * Creates a coroutine with receiver type [R] and result type [T].
 * This function creates a new, fresh instance of suspendable computation every time it is invoked.
 *
 * To start executing the created coroutine, invoke `resume(Unit)` on the returned [Continuation] instance.
 * The [completion] continuation is invoked when coroutine completes with result or exception.
 *
 * This function is _unchecked_. Repeated invocation of any resume function on the resulting continuation corrupts the
 * state machine of the coroutine and may result in arbitrary behaviour or exception.
 */
public fun <R, T> (suspend R.() -> T).createCoroutineUnchecked(
        receiver: R,
        completion: Continuation<T>
): Continuation<Unit> =
        if (this !is CoroutineImpl)
            buildContinuationByInvokeCall(completion) {
                (this as Function2<R, Continuation<T>, Any?>).invoke(receiver, completion)
            }
        else
            (this.create(receiver, completion) as CoroutineImpl).facade

// INTERNAL DEFINITIONS
// TODO: uncomment as soon as inlining works for stdlib.
@FixmeInline
private /*inline*/ fun <T> buildContinuationByInvokeCall(
        completion: Continuation<T>,
        /*crossinline*/ block: () -> Any?
): Continuation<Unit> {
    val continuation =
            object : Continuation<Unit> {
                override val context: CoroutineContext
                    get() = completion.context

                override fun resume(value: Unit) {
                    processBareContinuationResume(completion, block)
                }

                override fun resumeWithException(exception: Throwable) {
                    completion.resumeWithException(exception)
                }
            }

    return interceptContinuationIfNeeded(completion.context, continuation)
}

/**
 * Starts unintercepted coroutine without receiver and with result type [T] and executes it until its first suspension.
 * Returns the result of the coroutine or throws its exception if it does not suspend or [COROUTINE_SUSPENDED] if it suspends.
 * In the later case, the [completion] continuation is invoked when coroutine completes with result or exception.
 * This function is designed to be used from inside of [suspendCoroutineOrReturn] to resume the execution of suspended
 * coroutine using a reference to the suspending function.
 */
@Suppress("UNCHECKED_CAST")
@kotlin.internal.InlineOnly
public inline fun <T> (suspend () -> T).startCoroutineUninterceptedOrReturn(
        completion: Continuation<T>
): Any? = (this as Function1<Continuation<T>, Any?>).invoke(completion)

/**
 * Starts unintercepted coroutine with receiver type [R] and result type [T] and executes it until its first suspension.
 * Returns the result of the coroutine or throws its exception if it does not suspend or [COROUTINE_SUSPENDED] if it suspends.
 * In the later case, the [completion] continuation is invoked when coroutine completes with result or exception.
 * This function is designed to be used from inside of [suspendCoroutineOrReturn] to resume the execution of suspended
 * coroutine using a reference to the suspending function.
 */
@Suppress("UNCHECKED_CAST")
@kotlin.internal.InlineOnly
public inline fun <R, T> (suspend R.() -> T).startCoroutineUninterceptedOrReturn(
        receiver: R,
        completion: Continuation<T>
): Any? = (this as Function2<R, Continuation<T>, Any?>).invoke(receiver, completion)
