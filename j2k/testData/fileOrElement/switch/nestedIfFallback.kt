fun foo(i: Int, j: Int): String {
    when (i) {
        0 -> {
            return if (j > 0) {
                "1"
            } else "2"
        }
        1 -> return "2"
        else -> return "3"
    }
}