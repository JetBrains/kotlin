class Test {
    default object {
        public fun getInt(i: Int): Int {
            when (i) {
                0 -> return 0
                1 -> return 1
                2 -> return 2
                3 -> return 3
                else -> return -1
            }
        }
    }
}