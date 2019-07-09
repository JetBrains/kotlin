internal class Test {
    fun putInt(i: Int) {}

    fun test() {
        val b: Byte = 10
        putInt(b.toInt())
    }
}