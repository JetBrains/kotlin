class A {
    internal fun foo(min: Int) {
        for (i in 10 downTo min + 1) {
            println(i)
        }
    }
}
