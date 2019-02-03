package demo

internal class Test {
    fun test(vararg args: Any?) {
        var args: Any? = args
        args = arrayOf(1, 2, 3)
    }
}