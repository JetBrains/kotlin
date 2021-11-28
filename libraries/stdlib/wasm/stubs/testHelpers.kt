/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

import kotlin.internal.InlineOnly
import kotlin.internal.OnlyInputTypes

// TODO: Remove this once we support CLASS_REFERENCE IR node
@InlineOnly
public inline fun <reified T : Throwable> assertFailsWith(message: String? = null, block: () -> Unit): T {
    var throwable: Throwable? = null
    try {
        block()
    } catch (e: Throwable) {
        throwable = e
    }
    return throwable as? T ?: throw AssertionError("assertFailsWith failed: $message")
}
