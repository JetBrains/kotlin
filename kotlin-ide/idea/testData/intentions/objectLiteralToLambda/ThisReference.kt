// WITH_RUNTIME

class Test {
    fun foo() {
        bar(object : Runnable {
            override fun run() {
                println(this)
            }
        })

        // Applicable
        bar(<caret>object : Runnable {
            override fun run() {
                println(this@Test)
            }
        })

        // Applicable
        bar(object : Runnable {
            override fun run() {
                bar(object : Runnable {
                    override fun run() {
                        println(this)
                    }
                })
            }
        })
    }

    private fun bar(b: Runnable) {

    }
}