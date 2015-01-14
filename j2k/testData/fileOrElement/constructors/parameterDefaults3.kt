package pack


fun C(a: Int, b: Int, c: Int): C {
    return C(b, a, c, 0, 0)
}

class C(a: Int = 0, b: Int = 0, c: Int = 0, d: Int = 0, e: Int = 0)