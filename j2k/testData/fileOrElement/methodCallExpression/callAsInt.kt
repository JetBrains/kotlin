class Sample {
    fun foo() {
        val c = 'a'
        val b: Byte = 0
        val s: Short = 0
        bar(c.toInt())
        bar(b.toInt())
        bar(s.toInt())
    }

    fun bar(i: Int) {}
}