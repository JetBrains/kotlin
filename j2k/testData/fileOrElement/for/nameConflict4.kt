internal class A {
    fun foo(p: Boolean) {
        run {
            var i = 1
            while (i < 1000) {
                println(i)
                i *= 2
            }
        }

        if (p) {
            val i = 10
        }
    }
}
