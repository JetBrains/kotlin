// IS_APPLICABLE: false
// WITH_RUNTIME

fun foo() {
    <caret>object : Runnable {
        override fun run() {
            this.hashCode()
        }
    }.run()
}