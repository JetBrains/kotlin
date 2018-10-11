// EXPECTED_REACHABLE_NODES: 1237
package foo

var c: String = "fail3"
var d: String = "fail4"
var e: String = "fail5"
var f: String = "fail6"

fun cSet(s: String) { c = s }
fun dSet(s: String) { d = s }
fun eSet(s: String, b: Boolean): Boolean { e = s; return b }
fun fSet(s: String, b: Boolean): Boolean { f = s; return b }

open class Base(val a: String, val b: String) {
    fun foo() = (a + b + c + d + e + f)
}


class Derived(a1: String, a2: String, b1: String, b2: String, cond: Boolean) : Base(
    if (eSet("E_OK;", cond)) {
        cSet("C_THEN;")
        a1
    } else {
        cSet("C_ELSE;")
        a2
    },
    if (fSet("F_OK;", !cond)) {
        dSet("D_THEN;")
        b1
    } else {
        dSet("D_ELSE;")
        b2
    }
) {
}

fun box():String {
    val d1 = Derived("A_OK;", "A_FAIL;", "B_FAIL;", "B_OK;", true)
    assertEquals(d1.foo(), "A_OK;B_OK;C_THEN;D_ELSE;E_OK;F_OK;")

    c = "fail1"
    d = "fail2"
    e = "fail3"
    f = "fail4"

    val d2 = Derived("A_FAIL;", "A_OK;", "B_OK;", "B_FAIL;", false)
    assertEquals(d2.foo(), "A_OK;B_OK;C_ELSE;D_THEN;E_OK;F_OK;")

    return "OK"
}