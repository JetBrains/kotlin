/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package kotlin.coroutines.native.internal

// TODO: support coroutines debugging in Kotlin/Native.

import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.*

/**
 * This probe is invoked when coroutine is being created and it can replace completion
 * with its own wrapped object to intercept completion of this coroutine.
 *
 * This probe is invoked from stdlib implementation of [createCoroutineUnintercepted] function.
 *
 * Once created, coroutine is repeatedly [resumed][probeCoroutineResumed] and [suspended][probeCoroutineSuspended],
 * until it is complete. On completion, the object that was returned by this probe is invoked.
 *
 * ```
 * +-------+  probeCoroutineCreated +-----------+
 * | START | ---------------------->| SUSPENDED |
 * +-------+                        +-----------+
 *                probeCoroutineResumed |   ^ probeCoroutineSuspended
 *                                      V   |
 *                                  +------------+ completion invoked  +-----------+
 *                                  |   RUNNING  | ------------------->| COMPLETED |
 *                                  +------------+                     +-----------+
 * ```
 *
 * While the coroutine is resumed and suspended, it is represented by the pointer to its `frame`
 * which always extends [BaseContinuationImpl] and represents a pointer to the topmost frame of the
 * coroutine. Each [BaseContinuationImpl] object has [completion][BaseContinuationImpl.completion] reference
 * that points either to another frame (extending [BaseContinuationImpl]) or to the completion object
 * that was returned by this `probeCoroutineCreated` function.
 *
 * When coroutine is [suspended][probeCoroutineSuspended], then it is later [resumed][probeCoroutineResumed]
 * with a reference to the same frame. However, while coroutine is running it can unwind its frames and
 * invoke other suspending functions, so its next suspension can happen with a different frame pointer.
 */
@SinceKotlin("1.3")
internal fun <T> probeCoroutineCreated(completion: Continuation<T>): Continuation<T> {
    return completion
}

/**
 * This probe is invoked when coroutine is resumed using [Continuation.resumeWith].
 *
 * This probe is invoked from stdlib implementation of [BaseContinuationImpl.resumeWith] function.
 *
 * Coroutines machinery implementation guarantees that the actual [frame] instance extends
 * [BaseContinuationImpl] class, despite the fact that the declared type of [frame]
 * parameter in this function is `Continuation<*>`. See [probeCoroutineCreated] for details.
 */
@SinceKotlin("1.3")
internal fun probeCoroutineResumed(frame: Continuation<*>) {
}

/**
 * This probe is invoked when coroutine is suspended using [suspendCoroutineUninterceptedOrReturn], that is
 * when the corresponding `block` returns [COROUTINE_SUSPENDED].
 *
 * This probe is invoked from compiler-generated intrinsic for [suspendCoroutineUninterceptedOrReturn] function.
 *
 * Coroutines machinery implementation guarantees that the actual [frame] instance extends
 * [BaseContinuationImpl] class, despite the fact that the declared type of [frame]
 * parameter in this function is `Continuation<*>`. See [probeCoroutineCreated] for details.
 */
@SinceKotlin("1.3")
internal fun probeCoroutineSuspended(frame: Continuation<*>) {
}

