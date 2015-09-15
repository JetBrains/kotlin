package pack

internal class C @JvmOverloads internal constructor(a: Int = 0, b: Int = 0, c: Int = 0, d: Int = 0, e: Int = 0) {

    internal constructor(a1: Int, b1: Int, c1: Int) : this(a1, b1, c1, 0, 0) {
    }

    internal constructor(b: Byte) : this(b.toInt(), 0, 0, 0, 0) {
    }
}
