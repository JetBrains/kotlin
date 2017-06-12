// EXPECTED_REACHABLE_NODES: 502
package foo

class A {
    operator fun component1(): Int = 1
}
operator fun A.component2(): String = "n"

class B {
    operator fun component1(): Int = 1
    operator fun component2(): Int = 2
    operator fun component3(): Int = 3
}

class C {
    operator fun component1(): Int = 42
}

fun box(): String {
    val (a, b) = A()
    if (a != 1) return "a != 1, it: $a"
    if (b != "n") return "b != 'n', it: $b"

    var (x, y) = A()
    if (x != 1) return "x != 1, it: $x"
    if (y != "n") return "y != 'n', it: $y"

    x = 3
    if (x != 3) return "x != 3, it: $x"
    y = "m"
    if (y != "m") return "y != 'm', it: $y"

    var (b1, b2, b3) = B()
    if (b1 != 1) return "b1 != 1, it: $b1"
    if (b2 != 2) return "b2 != 2, it: $b2"
    if (b3 != 3) return "b3 != 3, it: $b3"

    val (c) = C()
    if (c != 42) return "c != 42, it: $c"

    return "OK"
}