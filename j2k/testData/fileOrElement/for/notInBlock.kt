class A {
    fun foo(p: Boolean) {
        if (p) {
            var i = 1
            while (i < 1000) {
                System.out.println(i)
                i *= 2
            }
        }
    }
}
