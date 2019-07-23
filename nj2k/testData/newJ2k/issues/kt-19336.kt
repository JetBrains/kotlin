class TestReturnsArray {
    fun strings(n: Int): Array<String?> {
        val result = arrayOfNulls<String>(n)
        for (i in 0 until n) {
            result[i] = Integer.toString(i)
        }
        return result
    }
}