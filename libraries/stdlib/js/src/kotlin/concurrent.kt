/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.contracts.*


@Deprecated("Use Synchronized annotation from kotlin.jvm package", ReplaceWith("kotlin.jvm.Synchronized"), level = DeprecationLevel.WARNING)
public typealias Synchronized = kotlin.jvm.Synchronized

@Deprecated("Use Volatile annotation from kotlin.jvm package", ReplaceWith("kotlin.jvm.Volatile"), level = DeprecationLevel.WARNING)
public typealias Volatile = kotlin.jvm.Volatile

@kotlin.internal.InlineOnly
public actual inline fun <R> synchronized(lock: Any, block: () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}
