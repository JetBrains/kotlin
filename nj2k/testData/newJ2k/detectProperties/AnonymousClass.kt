class X {
    internal fun foo() {
        val runnable: Runnable = object : Runnable {
            var value = 10

            override fun run() {
                println(value)
            }
        }
    }
}