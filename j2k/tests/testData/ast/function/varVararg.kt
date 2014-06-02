package demo

open class Test() {
    open fun test(vararg args: Any?) {
        args = array<Int?>(1, 2, 3)
    }
}