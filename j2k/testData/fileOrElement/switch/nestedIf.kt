fun foo(i: Int, j: Int): String {
    when (i) {
        0 -> if (j > 0) {
            return "1"
        } else {
            return "2"
        }
        1 -> return "3"
        else -> return "4"
    }
}