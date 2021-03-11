// IS_APPLICABLE: false

interface MyRunnable {
    fun run()
}

fun foo(runnable: MyRunnable) {}

fun bar() {
    foo(<caret>object : MyRunnable {
        override fun run() {
        }
    })
}