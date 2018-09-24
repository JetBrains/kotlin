class Sample {
    fun foo() {
        val map = arrayOf("FOO", "BAR")
        val c = '\u0000'
        val str = map[c.toInt()]
    }
}