package demo

open class Test(i: Int) {
    open fun test() {
        var b: Byte = 10
        Test(b.toInt())
    }
}