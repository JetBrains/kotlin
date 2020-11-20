/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines

@PublishedApi
@SinceKotlin("1.3")
internal actual class SafeContinuation<in T> : Continuation<T> {
    actual internal constructor(delegate: Continuation<T>, initialResult: Any?) { TODO("Wasm stdlib: Coroutines") }

    @PublishedApi
    actual internal constructor(delegate: Continuation<T>) { TODO("Wasm stdlib: Coroutines") }

    @PublishedApi
    actual internal fun getOrThrow(): Any? = TODO("Wasm stdlib: Coroutines")

    actual override val context: CoroutineContext = TODO("Wasm stdlib: Coroutines")
    actual override fun resumeWith(result: Result<T>): Unit { TODO("Wasm stdlib: Coroutines") }
}
