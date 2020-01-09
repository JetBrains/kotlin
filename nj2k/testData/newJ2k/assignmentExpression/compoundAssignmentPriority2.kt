object J {
    @JvmStatic
    fun main(args: Array<String>) {
        val a = 0
        val b = 1
        var c = 2
        c = c xor (a and b)
        print(c)
    }
}
