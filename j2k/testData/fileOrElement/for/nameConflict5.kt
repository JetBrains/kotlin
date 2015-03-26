class A {
    fun foo(p: Boolean) {
        if (p) {
            val i = 10
        }

        var i = 1
        while (i < 1000) {
            System.out.println(i)
            i *= 2
        }
    }
}
