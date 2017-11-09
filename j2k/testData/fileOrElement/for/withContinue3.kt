object TestClass {
    @JvmStatic
    fun main(args: Array<String>) {
        var i = 1
        while (i < 1000) {
            if (i == 4 || i == 8) {
                i *= 2
                continue
            }
            System.err.println(i)
            i *= 2
        }
    }
}