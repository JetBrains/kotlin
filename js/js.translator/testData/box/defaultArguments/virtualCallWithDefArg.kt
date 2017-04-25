// EXPECTED_REACHABLE_NODES: 506
package foo

interface A {
    fun bar2(arg: Int = 239): Int

    fun bar(arg: Int = 240): Int = bar2(arg / 2)
}

open abstract class B() : A {
    override fun bar2(arg: Int): Int = arg
}

class C() : B() {
}

fun box(): String {
    if (C().bar(10) != 5) return "fail1: ${C().bar(10)}"
    if (C().bar() != 120) return "fail2: ${C().bar()}"
    if (C().bar2() != 239) return "fail3: ${C().bar2()}"
    if (C().bar2(10) != 10) return "fail4: ${C().bar2(10)}"
    return "OK"
}
