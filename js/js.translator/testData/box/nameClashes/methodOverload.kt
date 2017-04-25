// EXPECTED_REACHABLE_NODES: 494
package foo

class A() {

    fun eval() = 3
    fun eval(a: Int) = 4
    fun eval(a: String) = 5
    fun eval(a: String, b: Int) = 6

}

fun box(): String {

    if (A().eval() != 3) return "fail1"
    if (A().eval(2) != 4) return "fail2"
    if (A().eval("3") != 5) return "fail3"
    if (A().eval("a", 3) != 6) return "fail4"

    return "OK"
}