class Test { // TARGET_BLOCK:
    fun method() = 1

    fun foo() {
        <caret>object : Runnable {
            override fun run() {
                method()
            }
        }
    }
}