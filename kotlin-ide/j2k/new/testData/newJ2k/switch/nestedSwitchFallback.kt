fun foo(i: Int, j: Int): String {
    return when (i) {
        0 -> {
            when (j) {
                1 -> return "0, 1"
                2 -> return "0, 2"
            }
            "1, x"
        }
        1 -> "1, x"
        else -> "x, x"
    }
}