internal class Test {
    fun printNumbers() {
        for (i1 in 0..0)
            println(i1)

        val b: Byte = 1
        for (i2 in 0 until b)
            println(i2)

        val s: Short = 1
        for (i3 in 0 until s)
            println(i3)

        val l = 1L
        for (i4 in 0 until l)
            println(i4)

        val d = 1.0
        var i5 = 0
        while (i5 < d) {
            println(i5)
            i5++
        }

        val f = 1.0f
        var i6 = 0
        while (i6 < f) {
            println(i6)
            i6++
        }

        val c: Char = 1.toChar()
        var i7 = 0
        while (i7 < c.toInt()) {
            println(i7)
            i7++
        }

    }
}