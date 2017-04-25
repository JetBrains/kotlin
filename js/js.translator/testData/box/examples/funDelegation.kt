// EXPECTED_REACHABLE_NODES: 504
open class Base() {
    fun n(n: Int): Int = n + 1
}

interface Abstract {
}

class Derived1() : Base(), Abstract {
}
class Derived2() : Abstract, Base() {
}

fun test(s: Base): Boolean = s.n(238) == 239

fun box(): String {
    if (!test(Base())) return "Fail #1"
    if (!test(Derived1())) return "Fail #2"
    if (!test(Derived2())) return "Fail #3"
    return "OK"
}