fun intarray_1(x: Int): Int {
    val z = IntArray(10)
    z.set(1, x)
    return z.get(1)
}