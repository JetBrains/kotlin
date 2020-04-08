class SomeClass {
    var a = 0
    var b = 0
    fun doSomeWhile(i: Int) {
        do {
            b = i
            a = b
        } while (i < 0)
    }
}