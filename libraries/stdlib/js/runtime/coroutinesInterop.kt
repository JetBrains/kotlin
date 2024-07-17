/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalJsCollectionsApi::class)

package kotlin.coroutines

import kotlin.js.Promise

internal fun <T> promisify(fn: suspend () -> T): Promise<T> =
    Promise { resolve, reject ->
        val completion = Continuation(EmptyCoroutineContext) {
            it.onSuccess(resolve).onFailure(reject)
        }
        fn.startCoroutine(completion)
    }

internal suspend fun <T> await(promise: Promise<T>): T = suspendCoroutine { continuation ->
    promise.then(
        onFulfilled = { result -> continuation.resume(result) },
        onRejected = { error -> continuation.resumeWithException(error) }
    )
}