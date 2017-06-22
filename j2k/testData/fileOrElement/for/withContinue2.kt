object TestClass {
    @JvmStatic
    fun main(args: Array<String>) {
        var i = 0
        var j = 1
        while (i < 10) {
            if (i == 4 || i == 8) {
                i++
                ++i
                j *= 2
                continue
            }
            System.err.println(j)
            ++i
            j *= 2
        }
    }
}