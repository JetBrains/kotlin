enum class Color : Runnable {
    WHITE
    BLACK
    RED
    YELLOW
    BLUE

    override fun run() {
        System.out.println("name()=" + name() + ", toString()=" + toString())
    }
}