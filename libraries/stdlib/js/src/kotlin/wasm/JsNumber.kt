/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/** JavaScript primitive number */
public external class JsNumber internal constructor() : JsAny

@Suppress("NOTHING_TO_INLINE")
public inline fun JsNumber.toDouble(): Double = unsafeCast<Double>()

@Suppress("NOTHING_TO_INLINE")
public inline fun Double.toJsNumber(): JsNumber = unsafeCast<JsNumber>()

@Suppress("NOTHING_TO_INLINE")
public inline fun JsNumber.toInt(): Int = unsafeCast<Int>()

@Suppress("NOTHING_TO_INLINE")
public inline fun Int.toJsNumber(): JsNumber = unsafeCast<JsNumber>()