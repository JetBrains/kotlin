/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines.cancellation

import kotlin.internal.InlineOnly

@SinceKotlin("1.4")
public actual typealias CancellationException = java.util.concurrent.CancellationException

@InlineOnly
@SinceKotlin("1.4")
public actual inline fun CancellationException(message: String?, cause: Throwable?): CancellationException {
    return CancellationException(message).also { it.initCause(cause) }
}
@InlineOnly
@SinceKotlin("1.4")
public actual inline fun CancellationException(cause: Throwable?): CancellationException {
    return CancellationException(cause?.toString()).also { it.initCause(cause) }
}