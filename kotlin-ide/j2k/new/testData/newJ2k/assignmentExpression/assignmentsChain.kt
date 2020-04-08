internal object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        var a = 0
        var b = 0
        var c = 0
        var d = 0
        val e = 0
        d *= e
        c = d
        b += c
        a = b
        //-----
        a = b
        //-----
        a += b
        //-----
        b = c
        a += b
        //-----
        b += c
        a = b
    }
}