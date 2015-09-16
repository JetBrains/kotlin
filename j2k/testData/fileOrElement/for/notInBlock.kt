internal class A {
    fun foo(p: Boolean) {
        if (p) {
            var i = 1
            while (i < 1000) {
                println(i)
                i *= 2
            }
        }
    }
}
