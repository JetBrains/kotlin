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

@file:kotlin.jvm.JvmName("CoroutinesJvmKt")
@file:kotlin.jvm.JvmVersion
package kotlin.coroutines.experimental

import kotlin.coroutines.experimental.jvm.internal.CoroutineImpl
import kotlin.coroutines.experimental.jvm.internal.interceptContinuationIfNeeded

@SinceKotlin("1.1")
internal fun <T> (suspend () -> T).createCoroutineInternal(
        completion: Continuation<T>
): Continuation<Unit> =
        if (this !is CoroutineImpl)
            buildContinuationByInvokeCall(completion) {
                (this as Function1<Continuation<T>, Any?>).invoke(completion)
            }
        else
            (this.create(completion) as CoroutineImpl).facade

@SinceKotlin("1.1")
internal fun <R, T> (suspend R.() -> T).createCoroutineInternal(
        receiver: R,
        completion: Continuation<T>
): Continuation<Unit> =
        if (this !is CoroutineImpl)
            buildContinuationByInvokeCall(completion) {
                (this as Function2<R, Continuation<T>, Any?>).invoke(receiver, completion)
            }
        else
            ((this as CoroutineImpl).create(receiver, completion) as CoroutineImpl).facade


private inline fun <T> buildContinuationByInvokeCall(
        completion: Continuation<T>,
        crossinline block: () -> Any?
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

    return completion.context.interceptContinuationIfNeeded(continuation)
}

