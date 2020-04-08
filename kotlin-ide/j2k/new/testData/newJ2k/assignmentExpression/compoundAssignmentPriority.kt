object J {
    @JvmStatic
    fun main(args: Array<String>) {
        val a = false
        val b = true
        var c = false
        c = c and (a || b)
        print(c)
    }
}
