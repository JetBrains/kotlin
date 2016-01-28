package kotlin

/**
 * Returns `true` if the specified number is a
 * Not-a-Number (NaN) value, `false` otherwise.
 */
public fun Double.isNaN(): Boolean = this != this

/**
 * Returns `true` if the specified number is a
 * Not-a-Number (NaN) value, `false` otherwise.
 */
public fun Float.isNaN(): Boolean = this != this

/**
 * Returns `true` if this value is infinitely large in magnitude.
 */
public fun Double.isInfinite(): Boolean = this == Double.POSITIVE_INFINITY || this == Double.NEGATIVE_INFINITY

/**
 * Returns `true` if this value is infinitely large in magnitude.
 */
public fun Float.isInfinite(): Boolean = this == Float.POSITIVE_INFINITY || this == Float.NEGATIVE_INFINITY

/**
 * Returns `true` if the argument is a finite floating-point value; returns `false` otherwise (for `NaN` and infinity arguments).
 */
public fun Double.isFinite(): Boolean = !isInfinite() && !isNaN()

/**
 * Returns `true` if the argument is a finite floating-point value; returns `false` otherwise (for `NaN` and infinity arguments).
 */
public fun Float.isFinite(): Boolean = !isInfinite() && !isNaN()
