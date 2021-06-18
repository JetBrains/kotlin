/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNUSED_PARAMETER", "NOTHING_TO_INLINE")

package kotlin.js

// Parameters are suffixed with `_hack` as a workaround for Namer.

/**
 * Function corresponding to JavaScript's `typeof` operator
 */
public fun jsTypeOf(value_hack: Any?): String =
    js("typeof value_hack").unsafeCast<String>()

@OptIn(JsIntrinsic::class)
internal inline fun jsDeleteProperty(obj: Any, property: Any) {
    jsDelete(obj.asDynamic()[property])
}

// Used in common stdlib code (reflection.kt)
@OptIn(JsIntrinsic::class)
internal inline fun jsBitwiseOr(lhs: Any?, rhs: Any?): Int = jsBitOr(lhs, rhs)