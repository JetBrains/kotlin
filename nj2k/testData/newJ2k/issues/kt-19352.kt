package test

class TestIntCompatibleAsArrayIndex {
    private val b: Byte = 0
    private val s: Short = 0
    private val ints = IntArray(4)
    fun foo(i: Int) {
        ints[b.toInt()] = i
        ints[s.toInt()] = i
    }
}