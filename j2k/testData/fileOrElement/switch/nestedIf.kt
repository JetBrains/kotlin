fun foo(i: Int, j: Int): String {
    when (i) {
        0 -> return if (j > 0) {
            "1"
        } else {
            "2"
        }
        1 -> return "3"
        else -> return "4"
    }
}