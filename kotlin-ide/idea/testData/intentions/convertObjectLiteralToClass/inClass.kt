class Test { // TARGET_BLOCK:
    fun foo() {
        <caret>object : Runnable {
            override fun run() {
            }
        }
    }
}