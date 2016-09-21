class Test {
    var someRunnable: Runnable = object : Runnable {
        override fun run() {
            this.run()
        }
    }
}

class Test2 {
    private val someRunnable = object : Runnable {
        override fun run() {
            this.run()
        }
    }
}
