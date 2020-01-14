internal class F {
    fun f1(p1: Int, p2: Int, p3: Int, p4: Int, vararg p5: Int) {}
    fun f2(array: IntArray) {
        f1(
                1, 2,
                3, 4,
                *array
        )
    }
}