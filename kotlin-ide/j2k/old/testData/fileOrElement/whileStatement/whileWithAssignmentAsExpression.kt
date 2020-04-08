class SomeClass {
    internal var a: Int = 0
    internal var b: Int = 0
    internal fun doSomeWhile(i: Int) {
        while (i < 0) {
            b = i
            a = b
        }
    }
}