package demo

internal class Test {
    internal fun test(vararg args: Any) {
        var args = args
        args = arrayOf(1, 2, 3)
    }
}