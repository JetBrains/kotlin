package demo

internal class Test {
    fun test(vararg args: Any) {
        var args = args
        args = arrayOf(1, 2, 3)
    }
}