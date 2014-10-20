package pack


fun C(a1: Int, b1: Int, c1: Int): C {
    return C(a1, b1, c1, 0, 0)
}

fun C(b: Byte): C {
    return C(b.toInt(), 0, 0, 0, 0)
}

class C(a: Int = 0, b: Int = 0, c: Int = 0, d: Int = 0, e: Int = 0)