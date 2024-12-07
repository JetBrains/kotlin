/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.contracts.contract

// Here are functions specialized with String type to avoid marking Any.toString and its overrides as reachable in DCE.
// Just making the declarations public is impossible since it may change the resolution in a user code. 
// TODO: investigate other ways to achieve the same, preferably covering not stdlib only but user cases too.

@PublishedApi
@kotlin.internal.InlineOnly
internal inline fun check(value: Boolean, lazyMessage: () -> String): Unit {
    contract {
        returns() implies value
    }
    if (!value) {
        val message = lazyMessage()
        throw IllegalStateException(message)
    }
}

@PublishedApi
@kotlin.internal.InlineOnly
internal inline fun error(message: String): Nothing = throw IllegalStateException(message)
