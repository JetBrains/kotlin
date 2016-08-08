fun booleanarray_1(x: Boolean): Boolean {
    val z = BooleanArray(10)
    z.set(1, x)
    return z.get(1)
}