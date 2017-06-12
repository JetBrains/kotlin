// EXPECTED_REACHABLE_NODES: 506
open class Base() {
    val plain = 239
    public val read: Int
        get() = 239

    public var readwrite: Int = 0
        get() = field + 1
        set(n: Int) {
            field = n
        }
}

interface Abstract {
}

class Derived1() : Base(), Abstract {
}
class Derived2() : Abstract, Base() {
}

fun code(s: Base): Int {
    if (s.plain != 239) return 1
    if (s.read != 239) return 2
    s.readwrite = 238
    if (s.readwrite != 239) return 3
    return 0
}

fun test(s: Base): Boolean = code(s) == 0

fun box(): String {
    if (!test(Base())) return "Fail #1"
    if (!test(Derived1())) return "Fail #2"
    if (!test(Derived2())) return "Fail #3"
    return "OK"
}