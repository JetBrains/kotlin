class SomeClass {
    internal fun doSomeIf(i: Int) {
        val a: Int
        val b: Int
        val c: Int
        if (i < 0) {
            b = i
            a = b
        } else {
            c = i
            b = c
        }
    }
}