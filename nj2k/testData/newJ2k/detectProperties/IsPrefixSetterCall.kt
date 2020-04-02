object TestSetter {
    private var isThing: Int
        private get() = 42
        private set(thing) {}

    @JvmStatic
    fun main(args: Array<String>) {
        isThing = 42
        println(isThing)
    }
}
