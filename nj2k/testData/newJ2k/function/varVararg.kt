package demo

internal class Test {
    fun test(vararg args: Any?) {
        var args = args
        args = arrayOf<Int?>(1, 2, 3)
    }
}