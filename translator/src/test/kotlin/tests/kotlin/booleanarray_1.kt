fun booleanarray_1_slave(x: Boolean): Boolean {
    val z = BooleanArray(10)
    z.set(1, x)
    return z.get(1)
}

fun booleanarray_1(x: Boolean): Int {
    return if (booleanarray_1_slave(x)) 1 else 0
}

