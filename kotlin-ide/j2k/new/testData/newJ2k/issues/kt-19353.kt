class TestLongOfTwoInts {
    fun foo(x1: Int, x2: Int): Long {
        return (x1 or (x2.toLong() shl 32).toInt()).toLong()
    }
}