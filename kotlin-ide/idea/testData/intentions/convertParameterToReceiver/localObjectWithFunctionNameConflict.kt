// SHOULD_FAIL_WITH: Parameter reference can't be safely replaced with this@foo since @foo is ambiguous in this context
fun foo(<caret>bar: Int) {
    object {
        fun foo() {
            bar + 1
        }
    }
}