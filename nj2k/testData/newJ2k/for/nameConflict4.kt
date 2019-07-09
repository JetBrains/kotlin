internal class A {
    fun foo(p: Boolean) {
        var i = 1
        while (i < 1000) {
            println(i)
            i *= 2
        }

        if (p) {
            val i = 10
        }
    }
}
