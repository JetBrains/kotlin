class SomeClass {
    internal fun doSomeFor() {
        var a: Int
        var b: Int
        for (i in 0 until 10) {
            b = i
            a = b
        }
    }
}