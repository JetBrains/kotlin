fun test(b: Boolean, n: Int): String {
    return if (b) {
        if (n == 0)
            "zero"
        else if (n == 1)
            "one"
        else <caret>if (n == 2)
            "two"
        else "unknown"
    } else {
        ""
    }
}