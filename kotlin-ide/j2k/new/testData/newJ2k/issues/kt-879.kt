internal object Test {
    fun getInt(i: Int): Int {
        return when (i) {
            0 -> 0
            1 -> 1
            2 -> 2
            3 -> 3
            else -> -1
        }
    }
}
