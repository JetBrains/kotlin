class C() {
    fun getInstance(): Runnable = C

    companion object: Runnable {
        override fun run(): Unit {
        }
    }
}

fun foo() = C().getInstance()
