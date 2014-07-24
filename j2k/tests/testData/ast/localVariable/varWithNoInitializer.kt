class C {
    fun foo(p: Boolean): Int {
        var a: Int
        var b: Int
        a = 10
        b = 5
        if (p) a = 5 else b = 10
        return a + b
    }
}
