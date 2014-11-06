package pack


fun C(a: Int): C {
    return C(a, 0, 0, 0, 1)
}

class C(a: Int = 0, b: Int = 0, c: Int = 0, d: Int = 0, e: Int = 0)