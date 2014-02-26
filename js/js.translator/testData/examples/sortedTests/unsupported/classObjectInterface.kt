class C() {
    fun getInstance(): Runnable = C

    class object: Runnable {
        override fun run(): Unit {
        }
    }
}

fun foo() = C().getInstance()
