/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.internal.NoInfer

/** Retrieves the closest context argument or implicit receiver in scope */
context(context: A) public fun <A> implicit(): @NoInfer A = context

/** Runs the specified [block] with [context] in scope */
@kotlin.internal.InlineOnly
public inline fun <A, R> context(context: A, block: context(A) () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block(context)
}

/** Runs the specified [block] with [a] and [b] in scope */
@kotlin.internal.InlineOnly
public inline fun <A, B, R> context(a: A, b: B, block: context(A, B) () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block(a, b)
}

/** Runs the specified [block] with [a], [b] and [c] in scope */
@kotlin.internal.InlineOnly
public inline fun <A, B, C, R> context(a: A, b: B, c: C, block: context(A, B, C) () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block(a, b, c)
}