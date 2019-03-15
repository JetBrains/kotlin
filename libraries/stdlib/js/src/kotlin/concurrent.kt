/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.contracts.*


@Deprecated("Do not use Synchronized annotation in pure Kotlin/JS code", level = DeprecationLevel.ERROR)
public typealias Synchronized = kotlin.jvm.Synchronized

@Deprecated("Do not use Volatile annotation in pure Kotlin/JS code", level = DeprecationLevel.ERROR)
public typealias Volatile = kotlin.jvm.Volatile

@kotlin.internal.InlineOnly
public actual inline fun <R> synchronized(lock: Any, block: () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}
