/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalJsExport::class)
package kotlin.coroutines.cancellation

import kotlin.internal.InlineOnly
import kotlin.internal.LowPriorityInOverloadResolution

@SinceKotlin("1.4")
@JsImplicitExport(couldBeConvertedToExplicitExport = true)
public actual open class CancellationException
// This one is needed only for the JS interop
@LowPriorityInOverloadResolution
public constructor(message: String? = VOID, cause: Throwable? = VOID) : IllegalStateException(message, cause) {

    @JsExport.Ignore
    public actual constructor() : this(message = VOID, cause = VOID)

    @JsExport.Ignore
    public actual constructor(message: String?) : this(message = message, cause = VOID)

    @JsExport.Ignore
    public constructor(cause: Throwable?) : this(message = VOID, cause = cause)
}

/**
 * Creates an instance of [CancellationException] with the given [message] and [cause].
 */
@SinceKotlin("1.4")
@Deprecated("Provided for expect-actual matching", level = DeprecationLevel.HIDDEN)
@InlineOnly
public actual inline fun CancellationException(message: String?, cause: Throwable?): CancellationException =
    CancellationException(message, cause)

/**
 * Creates an instance of [CancellationException] with the given [cause].
 */
@SinceKotlin("1.4")
@Deprecated("Provided for expect-actual matching", level = DeprecationLevel.HIDDEN)
@InlineOnly
public actual inline fun CancellationException(cause: Throwable?): CancellationException =
    CancellationException(cause)
