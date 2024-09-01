/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines.cancellation

@SinceKotlin("1.4")
public actual open class CancellationException : IllegalStateException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
    public constructor(message: String?, cause: Throwable?) : super(message, cause)
    public constructor(cause: Throwable?) : super(cause)
}

/**
 * Creates an instance of [CancellationException] with the given [message] and [cause].
 */
@SinceKotlin("1.4")
@Deprecated("Provided for expect-actual matching", level = DeprecationLevel.HIDDEN)
@kotlin.internal.InlineOnly
public actual inline fun CancellationException(message: String?, cause: Throwable?): CancellationException =
    CancellationException(message, cause)

/**
 * Creates an instance of [CancellationException] with the given [cause].
 */
@SinceKotlin("1.4")
@Deprecated("Provided for expect-actual matching", level = DeprecationLevel.HIDDEN)
@kotlin.internal.InlineOnly
public actual inline fun CancellationException(cause: Throwable?): CancellationException =
    CancellationException(cause)