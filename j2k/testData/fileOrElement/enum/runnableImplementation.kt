internal enum class Color : Runnable {
    WHITE, BLACK, RED, YELLOW, BLUE;

    override fun run() {
        println(
                "name()=" + name +
                        ", toString()=" + toString()
        )
    }
}