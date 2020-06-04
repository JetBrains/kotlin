// WITH_RUNTIME

fun bar() {
    Thread(<caret>object: Runnable {
        override fun run() {
            throw UnsupportedOperationException()
        }
    })
}