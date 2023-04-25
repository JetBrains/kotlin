/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.contracts.*


@DeprecatedSinceKotlin(warningSince = "1.6", errorSince = "1.9")
@Deprecated("Synchronization on any object is not supported in Kotlin/JS", ReplaceWith("run(block)"))
@kotlin.internal.InlineOnly
@Suppress("UNUSED_PARAMETER")
public inline fun <R> synchronized(lock: Any, block: () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}
