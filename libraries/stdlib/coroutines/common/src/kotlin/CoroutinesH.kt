/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines

@PublishedApi
@SinceKotlin("1.3")
internal expect class SafeContinuation<in T> : Continuation<T> {
    internal constructor(delegate: Continuation<T>, initialResult: Any?)

    @PublishedApi
    internal constructor(delegate: Continuation<T>)

    @PublishedApi
    internal fun getOrThrow(): Any?

    override val context: CoroutineContext
    override fun resumeWith(result: Result<T>): Unit
}
