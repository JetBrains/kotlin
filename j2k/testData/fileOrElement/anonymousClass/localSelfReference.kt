class Test {
    fun someMethod() {
        val someRunnable = object : Runnable {
            override fun run() {
                this.run()
            }
        }
    }
}