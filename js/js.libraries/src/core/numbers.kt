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

/**
 * Returns a bit representation of the specified floating-point value as [Long]
 * according to the IEEE 754 floating-point "double format" bit layout.
 */
@SinceKotlin("1.2")
@library("doubleToBits")
public fun Double.toBits(): Long = definedExternally

/**
 * Returns a bit representation of the specified floating-point value as [Long]
 * according to the IEEE 754 floating-point "double format" bit layout,
 * preserving `NaN` values exact layout.
 */
@SinceKotlin("1.2")
@library("doubleToRawBits")
public fun Double.toRawBits(): Long = definedExternally

/**
 * Returns the [Double] value corresponding to a given bit representation.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun Double.Companion.fromBits(bits: Long): Double = js("Kotlin").doubleFromBits(bits).unsafeCast<Double>()

/**
 * Returns a bit representation of the specified floating-point value as [Int]
 * according to the IEEE 754 floating-point "single format" bit layout.
 *
 * Note that in Kotlin/JS [Float] range is wider than "single format" bit layout can represent,
 * so some [Float] values may overflow, underflow or loose their accuracy after conversion to bits and back.
 */
@SinceKotlin("1.2")
@library("floatToBits")
public fun Float.toBits(): Int = definedExternally

/**
 * Returns a bit representation of the specified floating-point value as [Int]
 * according to the IEEE 754 floating-point "single format" bit layout,
 * preserving `NaN` values exact layout.
 *
 * Note that in Kotlin/JS [Float] range is wider than "single format" bit layout can represent,
 * so some [Float] values may overflow, underflow or loose their accuracy after conversion to bits and back.
 */
@SinceKotlin("1.2")
@library("floatToRawBits")
public fun Float.toRawBits(): Int = definedExternally

/**
 * Returns the [Float] value corresponding to a given bit representation.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun Float.Companion.fromBits(bits: Int): Float = js("Kotlin").floatFromBits(bits).unsafeCast<Float>()