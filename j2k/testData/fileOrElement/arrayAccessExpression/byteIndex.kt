class Sample {
    fun foo() {
        val map = arrayOf("FOO", "BAR")
        val b: Byte = 0
        val str = map[b.toInt()]
    }
}