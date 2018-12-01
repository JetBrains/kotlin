package demo

internal class Test {
    fun test(vararg args: Any?) {
        var args: Any? = args
        args = intArrayOf(1, 2, 3)
    }
}