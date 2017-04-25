// EXPECTED_REACHABLE_NODES: 498
package foo

open abstract class A() {
    abstract var pos: Int
}

class B() : A() {
    override var pos: Int = 2
}

fun box(): String {

    val a: A = B()
    if (a.pos != 2) {
        return "fail1: ${a.pos}"
    }
    if (B().pos != 2) {
        return "fail2"
    }
    a.pos = 3;
    if (a.pos != 3) {
        return "fail3: ${a.pos}"
    }
    return "OK"
}