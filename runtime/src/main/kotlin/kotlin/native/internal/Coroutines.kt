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

package kotlin.native.internal

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

@kotlin.internal.InlineOnly
@PublishedApi
internal inline suspend fun <T> suspendCoroutineUninterceptedOrReturn(crossinline block: (Continuation<T>) -> Any?): T =
        returnIfSuspended<T>(block(getContinuation<T>()))

@Intrinsic
@PublishedApi
internal external fun <T> getContinuation(): Continuation<T>

@kotlin.internal.InlineOnly
@PublishedApi
internal inline suspend fun getCoroutineContext(): CoroutineContext =
        getContinuation<Any?>().context

@Intrinsic
@PublishedApi
internal external suspend fun <T> returnIfSuspended(@Suppress("UNUSED_PARAMETER") argument: Any?): T
