class Test { // TARGET_BLOCK:
    var field = 1

    fun foo() {
        <caret>object : Runnable {
            override fun run() {
                field = 2
            }
        }
    }
}