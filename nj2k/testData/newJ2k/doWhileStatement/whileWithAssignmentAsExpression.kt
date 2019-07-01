class SomeClass {
    internal var a = 0
    internal var b = 0
    internal fun doSomeWhile(i: Int) {
        do {
            b = i
            a = b
        } while (i < 0)
    }
}