fun test(a: Any, b: Boolean): Int  {
    when (a) {
        is Int -> {
            <caret>if (b) return 1
        }
    }
    return 0
}