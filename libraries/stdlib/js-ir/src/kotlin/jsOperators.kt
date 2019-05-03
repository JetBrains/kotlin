/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNUSED_PARAMETER")

package kotlin.js

// Parameters are suffixed with `_hack` as a workaround for Namer.
// TODO: Implemet as compiler intrinsics

/**
 * Function corresponding to JavaScript's `typeof` operator
 */
public fun jsTypeOf(value_hack: Any?): String =
    js("typeof value_hack").unsafeCast<String>()

internal fun jsDeleteProperty(obj_hack: Any, property_hack: Any) {
    js("delete obj_hack[property_hack]")
}

internal fun jsBitwiseOr(lhs_hack: Any?, rhs_hack: Any?): Int =
    js("lhs_hack | rhs_hack").unsafeCast<Int>()

internal fun jsBitwiseAnd(lhs_hack: Any?, rhs_hack: Any?): Int =
    js("lhs_hack & rhs_hack").unsafeCast<Int>()

internal fun jsInstanceOf(obj_hack: Any?, jsClass_hack: Any?): Boolean =
    js("obj_hack instanceof jsClass_hack").unsafeCast<Boolean>()

// Returns true if the specified property is in the specified object or its prototype chain.
internal fun jsIn(lhs_hack: Any?, rhs_hack: Any): Boolean =
    js("lhs_hack in rhs_hack").unsafeCast<Boolean>()

