fun foo(b: Boolean): Int {
    <caret>return when (b) {
        true -> throw AssertionError()
        else -> 1
    }
}