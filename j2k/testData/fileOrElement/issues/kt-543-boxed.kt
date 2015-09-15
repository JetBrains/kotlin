package demo

internal class Test {
    internal fun putInt(i: Int?) {
    }

    internal fun test() {
        val b = 10
        putInt(b.toInt())

        val b2 = 10
        putInt(b2.toInt())
    }
}