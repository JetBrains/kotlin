/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/**
 * Exposes the JavaScript [Math object](https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Global_Objects/Math) to Kotlin.
 */
@PublishedApi
@JsName("Math")
internal external object JsMath {
    val LN2: Double
    fun abs(value: Double): Double
    fun acos(value: Double): Double
    fun asin(value: Double): Double
    fun atan(value: Double): Double
    fun atan2(y: Double, x: Double): Double
    fun cos(value: Double): Double
    fun sin(value: Double): Double
    fun exp(value: Double): Double
    fun max(vararg values: Int): Int
    fun max(vararg values: Float): Float
    fun max(vararg values: Double): Double
    fun min(vararg values: Int): Int
    fun min(vararg values: Float): Float
    fun min(vararg values: Double): Double
    fun sqrt(value: Double): Double
    fun tan(value: Double): Double
    fun log(value: Double): Double
    fun pow(base: Double, exp: Double): Double
    fun round(value: Number): Double
    fun floor(value: Number): Double
    fun ceil(value: Number): Double

    fun trunc(value: Number): Double
    fun sign(value: Number): Double

    fun sinh(value: Double): Double
    fun cosh(value: Double): Double
    fun tanh(value: Double): Double
    fun asinh(value: Double): Double
    fun acosh(value: Double): Double
    fun atanh(value: Double): Double

    fun hypot(x: Double, y: Double): Double

    fun expm1(value: Double): Double
    fun log10(value: Double): Double
    fun log2(value: Double): Double
    fun log1p(value: Double): Double

    fun clz32(value: Int): Int
}

