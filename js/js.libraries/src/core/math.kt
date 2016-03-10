package kotlin.js

//TODO: declare using number
@native
public class MathClass() {
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
    public fun max(vararg values: Double): Double = noImpl
    public fun max(vararg values: Int): Int = noImpl
    public fun min(vararg values: Int): Int = noImpl
    public fun min(vararg values: Double): Double = noImpl
    public fun sqrt(value: Double): Double = noImpl
    public fun tan(value: Double): Double = noImpl
    public fun log(value: Double): Double = noImpl
    public fun pow(base: Double, exp: Double): Double = noImpl
    public fun round(value: Number): Int = noImpl
    public fun floor(value: Number): Int = noImpl
    public fun ceil(value: Number): Int = noImpl
}

@native
public val Math: MathClass = MathClass()
