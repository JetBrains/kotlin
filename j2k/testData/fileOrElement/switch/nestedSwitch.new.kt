fun foo(i: Int, j: Int): String {
    return when (i) {
        0 -> when (j) {
            1 -> "0, 1"
            else -> "0, x"
        }

        1 -> "1, x"
        else -> "x, x"
    }
}