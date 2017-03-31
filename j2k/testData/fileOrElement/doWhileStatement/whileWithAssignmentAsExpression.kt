class SomeClass {
    internal var a: Int = 0
    internal var b: Int = 0
    internal fun doSomeWhile(i: Int) {
        do {
            b = i
            a = b
        } while (i < 0)
    }
}