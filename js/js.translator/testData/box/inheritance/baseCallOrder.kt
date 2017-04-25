// EXPECTED_REACHABLE_NODES: 497
package foo

var x = false

open class A {
    init {
        x = (this as B).a != 3
    }
}

class B(val a: Int = 3) : A() {

}

fun box(): String {
    B()
    if (!x) return "fail"
    return "OK"
}