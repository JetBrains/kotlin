package vector

import kotlinx.cinterop.*

fun createVector(f0: Float, f1: Float, f2: Float, f3: Float): Vector128 = vectorOf(f0, f1, f2, f3)

fun Vector128.sumVectorFloat(): Float = getFloatAt(0) + getFloatAt(1) + getFloatAt(2) + getFloatAt(3)

fun createVector(i0: Int, i1: Int, i2: Int, i3: Int): Vector128 = vectorOf(i0, i1, i2, i3)

fun Vector128.sumVectorInt(): Int = getIntAt(0) + getIntAt(1) + getIntAt(2) + getIntAt(3)

fun createNullableVector(isNull: Boolean): Vector128? = if (isNull) null else vectorOf(1, 2, 3, 4)

fun Vector128?.sumNullableVectorInt(): Int = this?.sumVectorInt() ?: 0

var vector: Vector128 = vectorOf(0, 0, 0, 0)
