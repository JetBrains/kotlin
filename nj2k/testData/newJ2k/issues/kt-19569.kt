class TestJavaBoxedPrimitives {
    fun foo(
            x1: Boolean, x2: Byte, x3: Short, x4: Int,
            x5: Long, x6: Float, x7: Double, x8: Char
    ): Array<Any> {
        return arrayOf(x1, x2, x3, x4, x5, x6, x7, x8)
    }
}