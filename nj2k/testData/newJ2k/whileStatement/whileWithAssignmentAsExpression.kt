class SomeClass {
    var a = 0
    var b = 0
    fun doSomeWhile(i: Int) {
        while (i < 0) {
            b = i
            a = b
        }
    }
}