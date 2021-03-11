fun foo(n: Int) {
    val x = <caret>object {
        fun bar() = n
    }
}