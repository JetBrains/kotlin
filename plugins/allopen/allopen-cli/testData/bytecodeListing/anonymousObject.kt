// IGNORE_BACKEND_FIR: JVM_IR
//   FIR version does not go inside bodies
//   Also it's quiestionable do we even need to transform local classes and anonymous objects
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
