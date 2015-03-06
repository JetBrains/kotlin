package foo

class A {
    val a = 3
    default object {
        val a = 2
        val b = 5
    }
}


fun box(): String {
    if (A.a != 2) return "A.a != 2, it: ${A.a}"
    if (A.b != 5) return "A.b != 5, it: ${A.b}"

    val b = A
    if (b.a != 2) return "b = A; b != 2, it: ${b.a}"

    if (A().a != 3) return "A().a != 3, it: ${A().a}"

    return "OK"
}
