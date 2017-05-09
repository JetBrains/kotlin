fun foo(i: Int, j: Int): String {
    when (i) {
        0 -> {
            when (j) {
                1 -> return "0, 1"
                2 -> return "0, 2"
            }
            return "1, x"
        }
        1 -> return "1, x"
        else -> return "x, x"
    }
}