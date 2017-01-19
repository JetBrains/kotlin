package kotlin.js

//TODO: declare using number
public external object Math {
    public val PI: Double = noImpl
    public fun random(): Double = noImpl
    public fun abs(value: Double): Double = noImpl
    public fun acos(value: Double): Double = noImpl
    public fun asin(value: Double): Double = noImpl
    public fun atan(value: Double): Double = noImpl
    public fun atan2(x: Double, y: Double): Double = noImpl
    public fun cos(value: Double): Double = noImpl
    public fun sin(value: Double): Double = noImpl
    public fun exp(value: Double): Double = noImpl
    public fun max(vararg values: Int): Int = noImpl
    public fun max(vararg values: Float): Float = noImpl
    public fun max(vararg values: Double): Double = noImpl
    public fun min(vararg values: Int): Int = noImpl
    public fun min(vararg values: Float): Float = noImpl
    public fun min(vararg values: Double): Double = noImpl
    public fun sqrt(value: Double): Double = noImpl
    public fun tan(value: Double): Double = noImpl
    public fun log(value: Double): Double = noImpl
    public fun pow(base: Double, exp: Double): Double = noImpl
    public fun round(value: Number): Int = noImpl
    public fun floor(value: Number): Int = noImpl
    public fun ceil(value: Number): Int = noImpl
}

public fun Math.min(a: Long, b: Long): Long = if (a <= b) a else b
public fun Math.max(a: Long, b: Long): Long = if (a >= b) a else b