package algorithm.geometry

private val eps = 0.1

fun Double.lt(other: Double): Boolean {
    return this - other < -eps
}

fun Double.gt(other: Double): Boolean {
    return this - other > eps
}

fun Double.eq(other: Double): Boolean {
    return Math.abs(this - other) < eps
}