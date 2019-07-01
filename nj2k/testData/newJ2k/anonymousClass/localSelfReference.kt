class Test {
    fun someMethod() {
        val someRunnable: Runnable = object : Runnable {
            override fun run() {
                this.run()
            }
        }
    }
}