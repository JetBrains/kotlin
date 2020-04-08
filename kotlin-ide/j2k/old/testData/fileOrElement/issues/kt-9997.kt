internal class A {
    fun equals() {
        val i = 1
        val b: Byte = 1
        val s: Short = 1
        val l: Long = 1
        val d = 1.0
        val f = 1.0f
        val c: Char = 1.toChar()

        t(i == i)
        t(i == b.toInt())
        t(i == s.toInt())
        t(i.toLong() == l)
        t(i.toDouble() == d)
        t(i.toFloat() == f)
        t(i == c.toInt())

        t(b.toInt() == i)
        t(b == b)
        t(b.toShort() == s)
        t(b.toLong() == l)
        t(b.toDouble() == d)
        t(b.toFloat() == f)
        t(b == c.toByte())

        t(s.toInt() == i)
        t(s == b.toShort())
        t(s == s)
        t(s.toLong() == l)
        t(s.toDouble() == d)
        t(s.toFloat() == f)
        t(s == c.toShort())

        t(l == i.toLong())
        t(l == b.toLong())
        t(l == s.toLong())
        t(l == l)
        t(l.toDouble() == d)
        t(l.toFloat() == f)
        t(l == c.toLong())

        t(d == i.toDouble())
        t(d == b.toDouble())
        t(d == s.toDouble())
        t(d == l.toDouble())
        t(d == d)
        t(d == f.toDouble())
        t(d == c.toDouble())

        t(f == i.toFloat())
        t(f == b.toFloat())
        t(f == s.toFloat())
        t(f == l.toFloat())
        t(f.toDouble() == d)
        t(f == f)
        t(f == c.toFloat())

        t(c.toInt() == i)
        t(c.toByte() == b)
        t(c.toShort() == s)
        t(c.toLong() == l)
        t(c.toDouble() == d)
        t(c.toFloat() == f)
        t(c == c)

        t(i.toDouble() != d)
    }

    fun compare() {
        val i = 1
        val b: Byte = 1
        val s: Short = 1
        val l: Long = 1
        val d = 1.0
        val f = 1.0f
        val c: Char = 1.toChar()

        t(i > i)
        t(i > b)
        t(i > s)
        t(i > l)
        t(i > d)
        t(i > f)
        t(i > c.toInt())

        t(b > i)
        t(b > b)
        t(b > s)
        t(b > l)
        t(b > d)
        t(b > f)
        t(b > c.toByte())

        t(s > i)
        t(s > b)
        t(s > s)
        t(s > l)
        t(s > d)
        t(s > f)
        t(s > c.toShort())

        t(l > i)
        t(l > b)
        t(l > s)
        t(l > l)
        t(l > d)
        t(l > f)
        t(l > c.toLong())

        t(d > i)
        t(d > b)
        t(d > s)
        t(d > l)
        t(d > d)
        t(d > f)
        t(d > c.toDouble())

        t(f > i)
        t(f > b)
        t(f > s)
        t(f > l)
        t(f > d)
        t(f > f)
        t(f > c.toFloat())

        t(c.toInt() > i)
        t(c.toByte() > b)
        t(c.toShort() > s)
        t(c.toLong() > l)
        t(c.toDouble() > d)
        t(c.toFloat() > f)
        t(c > c)
    }

    private fun t(b: Boolean) {}
}