fun foo(i: Int, j: Int): String {
    when (i) {
        0 -> when (j) {
            1 -> return "0, 1"
            else -> return "0, x"
        }
        1 -> return "1, x"
        else -> return "x, x"
    }
}