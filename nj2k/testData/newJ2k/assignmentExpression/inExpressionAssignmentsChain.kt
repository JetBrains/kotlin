internal object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        var a = true
        var b = true
        var c = true
        var d = true
        val e = true
        if (e.let { d = d and it; d }.also { c = it }.let { b = b or it; b }.also { a = it });
        while (b.also { a = it });
        do {
        } while (b.let { a = a xor it; a })
        println(c.also { b = it }.let { a = a and it; a })
        println(b != c.also { a = it })
    }
}