class X {
    internal fun foo() {
        val runnable = object : Runnable {
            internal var value = 10

            override fun run() {
                println(value)
            }
        }
    }
}
