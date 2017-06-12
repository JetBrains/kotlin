// EXPECTED_REACHABLE_NODES: 509
package foo

open class A(val barVal: Int) {
    open fun bar(): Int = 1
    open fun bar2(): Int = barVal + 1
    open fun bar3(t: Int) = t + 2
    open fun bar4(t: Int = 1) = t + 3 + barVal
}

class B : A(1) {
    override fun bar(): Int {
        return super.bar() + 10;
    }
    override fun bar2(): Int {
        return super<A>.bar2() + 10;
    }
    override fun bar3(t: Int): Int {
        return super.bar3(t + 10);
    }
    override fun bar4(t: Int): Int {
        return super<A>.bar4() + 10;
    }
}

fun box(): String {
    val b = B()
    if (b.bar() != 11) return "Simple call fail. b.bar() is ${b.bar()}"
    if (b.bar2() != 12) return "Wrong 'this' in supercall. b.bar2() is ${b.bar2()}"
    if (b.bar3(2) != 14) return "Supercall with parameter fail. b.bar3(2) is ${b.bar3(2)}"
    if (b.bar4() != 15) return "Supercall with default parameter & this fault. b.bar4() is ${b.bar4()}"
    return "OK"
}