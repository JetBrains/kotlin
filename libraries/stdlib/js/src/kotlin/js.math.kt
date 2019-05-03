/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/**
 * Exposes the JavaScript [Math object](https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Global_Objects/Math) to Kotlin.
 */
@Deprecated("Use top-level functions from kotlin.math package instead.", level = DeprecationLevel.WARNING)
public external object Math {
    @Deprecated("Use kotlin.math.PI instead.", ReplaceWith("PI", "kotlin.math.PI"), level = DeprecationLevel.ERROR)
    public val PI: Double
    @Deprecated("Use Random.nextDouble instead", ReplaceWith("kotlin.random.Random.nextDouble()", "kotlin.random.Random"), level = DeprecationLevel.WARNING)
    public fun random(): Double
    @Deprecated("Use kotlin.math.abs instead.", ReplaceWith("abs(value)", "kotlin.math.abs"), level = DeprecationLevel.ERROR)
    public fun abs(value: Double): Double
    @Deprecated("Use kotlin.math.acos instead.", ReplaceWith("acos(value)", "kotlin.math.acos"), level = DeprecationLevel.ERROR)
    public fun acos(value: Double): Double
    @Deprecated("Use kotlin.math.asin instead.", ReplaceWith("asin(value)", "kotlin.math.asin"), level = DeprecationLevel.ERROR)
    public fun asin(value: Double): Double
    @Deprecated("Use kotlin.math.atan instead.", ReplaceWith("atan(value)", "kotlin.math.atan"), level = DeprecationLevel.ERROR)
    public fun atan(value: Double): Double
    @Deprecated("Use kotlin.math.atan2 instead.", ReplaceWith("atan2(y, x)", "kotlin.math.atan2"), level = DeprecationLevel.ERROR)
    public fun atan2(y: Double, x: Double): Double
    @Deprecated("Use kotlin.math.cos instead.", ReplaceWith("cos(value)", "kotlin.math.cos"), level = DeprecationLevel.ERROR)
    public fun cos(value: Double): Double
    @Deprecated("Use kotlin.math.sin instead.", ReplaceWith("sin(value)", "kotlin.math.sin"), level = DeprecationLevel.ERROR)
    public fun sin(value: Double): Double
    @Deprecated("Use kotlin.math.exp instead.", ReplaceWith("exp(value)", "kotlin.math.exp"), level = DeprecationLevel.ERROR)
    public fun exp(value: Double): Double
    @Deprecated("Use maxOf or kotlin.math.max instead", level = DeprecationLevel.ERROR)
    public fun max(vararg values: Int): Int
    @Deprecated("Use maxOf or kotlin.math.max instead", level = DeprecationLevel.ERROR)
    public fun max(vararg values: Float): Float
    @Deprecated("Use maxOf or kotlin.math.max instead", level = DeprecationLevel.ERROR)
    public fun max(vararg values: Double): Double
    @Deprecated("Use minOf or kotlin.math.min instead", level = DeprecationLevel.ERROR)
    public fun min(vararg values: Int): Int
    @Deprecated("Use minOf or kotlin.math.min instead", level = DeprecationLevel.ERROR)
    public fun min(vararg values: Float): Float
    @Deprecated("Use minOf or kotlin.math.min instead", level = DeprecationLevel.ERROR)
    public fun min(vararg values: Double): Double
    @Deprecated("Use kotlin.math.sqrt instead.", ReplaceWith("sqrt(value)", "kotlin.math.sqrt"), level = DeprecationLevel.ERROR)
    public fun sqrt(value: Double): Double
    @Deprecated("Use kotlin.math.tan instead.", ReplaceWith("tan(value)", "kotlin.math.tan"), level = DeprecationLevel.ERROR)
    public fun tan(value: Double): Double
    @Deprecated("Use kotlin.math.ln instead.", ReplaceWith("ln(value)", "kotlin.math.ln"), level = DeprecationLevel.ERROR)
    public fun log(value: Double): Double
    @Deprecated("Use kotlin.math.pow instead.", ReplaceWith("pow(base, exp)", "kotlin.math.pow"), level = DeprecationLevel.ERROR)
    public fun pow(base: Double, exp: Double): Double
    @Deprecated("Use kotlin.math.round instead.", ReplaceWith("round(value)", "kotlin.math.round"), level = DeprecationLevel.ERROR)
    public fun round(value: Number): Int
    @Deprecated("Use kotlin.math.floor instead.", ReplaceWith("floor(value)", "kotlin.math.floor"), level = DeprecationLevel.ERROR)
    public fun floor(value: Number): Int
    @Deprecated("Use kotlin.math.ceil instead.", ReplaceWith("ceil(value)", "kotlin.math.ceil"), level = DeprecationLevel.ERROR)
    public fun ceil(value: Number): Int

    @PublishedApi
    internal fun trunc(value: Number): Double
    @PublishedApi
    internal fun sign(value: Number): Double

    @PublishedApi
    internal fun sinh(value: Double): Double
    @PublishedApi
    internal fun cosh(value: Double): Double
    @PublishedApi
    internal fun tanh(value: Double): Double
    @PublishedApi
    internal fun asinh(value: Double): Double
    @PublishedApi
    internal fun acosh(value: Double): Double
    @PublishedApi
    internal fun atanh(value: Double): Double

    @PublishedApi
    internal fun hypot(x: Double, y: Double): Double

    @PublishedApi
    internal fun expm1(value: Double): Double

    @PublishedApi
    internal fun log10(value: Double): Double
    @PublishedApi
    internal fun log2(value: Double): Double
    @PublishedApi
    internal fun log1p(value: Double): Double
}

/**
 * Returns the smaller of two values.
 */
@Suppress("DEPRECATION")
@Deprecated("Use minOf or kotlin.math.min instead", ReplaceWith("minOf(a, b)"), level = DeprecationLevel.ERROR)
public fun Math.min(a: Long, b: Long): Long = if (a <= b) a else b

/**
 * Returns the greater of two values.
 */
@Suppress("DEPRECATION")
@Deprecated("Use maxOf or kotlin.math.max instead", ReplaceWith("maxOf(a, b)"), level = DeprecationLevel.ERROR)
public fun Math.max(a: Long, b: Long): Long = if (a >= b) a else b
