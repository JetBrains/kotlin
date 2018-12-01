package demo

internal class Test {
    fun putInt(i: Int?) {}

    fun test() {
        val b: Byte = 10
        putInt(b.toInt())

        val b2: Byte = 10
        putInt(b2.toInt())
    }
}