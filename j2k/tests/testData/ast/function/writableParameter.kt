package demo

open class Test() {
    open fun test(i: Int): Int {
        i = 10
        return i + 20
    }
}