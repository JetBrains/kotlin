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

/**
 * Starts unintercepted coroutine without receiver and with result type [T] and executes it until its first suspension.
 * Returns the result of the coroutine or throws its exception if it does not suspend or [COROUTINE_SUSPENDED] if it suspends.
 * In the later case, the [completion] continuation is invoked when coroutine completes with result or exception.
 * This function is designed to be used from inside of [suspendCoroutineOrReturn] to resume the execution of suspended
 * coroutine using a reference to the suspending function.
 */
@SinceKotlin("1.1")
public expect inline fun <T> (suspend () -> T).startCoroutineUninterceptedOrReturn(
        completion: Continuation<T>
): Any?

/**
 * Starts unintercepted coroutine with receiver type [R] and result type [T] and executes it until its first suspension.
 * Returns the result of the coroutine or throws its exception if it does not suspend or [COROUTINE_SUSPENDED] if it suspends.
 * In the later case, the [completion] continuation is invoked when coroutine completes with result or exception.
 * This function is designed to be used from inside of [suspendCoroutineOrReturn] to resume the execution of suspended
 * coroutine using a reference to the suspending function.
 */
@SinceKotlin("1.1")
public expect inline fun <R, T> (suspend R.() -> T).startCoroutineUninterceptedOrReturn(
        receiver: R,
        completion: Continuation<T>
): Any?

@SinceKotlin("1.1")
public expect fun <T> (suspend () -> T).createCoroutineUnchecked(
        completion: Continuation<T>
): Continuation<Unit>

@SinceKotlin("1.1")
public expect fun <R, T> (suspend R.() -> T).createCoroutineUnchecked(
        receiver: R,
        completion: Continuation<T>
): Continuation<Unit>
