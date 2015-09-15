package demo

internal class Test {
    internal fun getInteger(i: Int?): Int? {
        return i
    }

    internal fun test() {
        val i = getInteger(10)!!
    }
}