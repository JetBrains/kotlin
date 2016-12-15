annotation class AllOpen

@AllOpen
class Test {
    val a = object : Runnable {
        override fun run() {
            1
        }
    }

    fun b() {
        object : Runnable {
            override fun run() {
                1
            }
        }

        Runnable { 1 }.run()
    }
}