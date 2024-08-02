/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines.intrinsics

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineImpl

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal actual fun <T> createSimpleCoroutineForSuspendFunction(
    completion: Continuation<T>
): Continuation<T> = object : CoroutineImpl(completion as Continuation<Any?>) {
    override fun doResume(): Any? {
        exception?.let { throw it }
        return result
    }
}
