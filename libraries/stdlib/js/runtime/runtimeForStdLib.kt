/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.js

// Inlined intrinsics for backward compatibility with already implemented functions in stdlib for old JS backend
@OptIn(JsIntrinsic::class)
@kotlin.internal.InlineOnly
internal inline fun jsDeleteProperty(obj: dynamic, property: Any) = jsDelete(obj[property])

@OptIn(JsIntrinsic::class)
@kotlin.internal.InlineOnly
internal inline fun jsBitwiseOr(lhs: Any?, rhs: Any?): Int = jsBitOr(lhs, rhs)

@OptIn(JsIntrinsic::class)
@kotlin.internal.InlineOnly
internal inline fun jsBitwiseAnd(lhs: Any?, rhs: Any?): Int = jsBitAnd(lhs, rhs)

@OptIn(JsIntrinsic::class)
@kotlin.internal.InlineOnly
internal inline fun jsInstanceOf(obj: Any?, jsClass: Any?): Boolean = jsInstanceOfIntrinsic(obj, jsClass)

@OptIn(JsIntrinsic::class)
@kotlin.internal.InlineOnly
internal inline fun jsIn(lhs: Any?, rhs: Any): Boolean = jsInIntrinsic(lhs, rhs)
