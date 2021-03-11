// IS_APPLICABLE: false
// WITH_RUNTIME

fun foo(runnable: Runnable) {}

fun bar() {
    foo(<caret>object : Runnable {
        override fun run() {
            f()
        }

        fun f() {}
    })
}