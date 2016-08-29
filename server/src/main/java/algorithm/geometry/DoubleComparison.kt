package algorithm.geometry

fun Double.lt(other: Double): Boolean {
    return this - other < -Util.eps
}

fun Double.gt(other: Double): Boolean {
    return this - other > Util.eps
}

fun Double.eq(other: Double): Boolean {
    return Math.abs(this - other) < Util.eps;
}