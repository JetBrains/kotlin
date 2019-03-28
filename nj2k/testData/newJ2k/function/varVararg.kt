package demo

internal class Test {
    fun test(vararg args: Any?) {
        var argsx = args
        args = arrayOf(1, 2, 3)
    }
}