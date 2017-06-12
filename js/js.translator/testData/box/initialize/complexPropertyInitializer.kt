// EXPECTED_REACHABLE_NODES: 492
package foo

fun f() {}

class A(selector: Boolean, val y: Int) {
    val x = if (selector) { f(); y } else 999
    val z = if (selector) { f(); x + 1 } else 999
}

class B(selector: Boolean, val y: Int, val x: Int = if (selector) { f(); y } else { 999 })

fun box(): String {
    val a = A(true, 23)
    if (a.x != 23) return "fail: wrong evaluation order for property initializer (1): ${a.x}"
    if (a.z != 24) return "fail: wrong evaluation order for property initializer (2): ${a.z}"

    val b = B(true, 23)
    if (b.x != 23) return "fail: wrong evaluation order for default constructor arguments"

    return "OK"
}