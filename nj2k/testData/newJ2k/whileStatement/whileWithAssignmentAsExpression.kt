class SomeClass {
    internal var a = 0
    internal var b = 0
    internal fun doSomeWhile(i: Int) {
        while (i < 0) {
            b = i
            a = b
        }
    }
}