// EXPECTED_REACHABLE_NODES: 497
package foo

var sideEffect: Int = 0;

class A(val x: Int)

val global_a = A(1)
val global_b = A(2)

fun nullFun(value: Boolean): Any? = if (value) null else global_a

fun box(): String {

    nullFun(false) ?: global_b
    assertEquals(global_a, nullFun(false) ?: try { ++sideEffect; global_b } finally {}, "false, global_b")
    assertEquals(0, sideEffect, "false, global_b side effect")

    assertEquals(global_b, nullFun(true) ?: try { ++sideEffect; global_b } finally {}, "true, global_b")
    assertEquals(1, sideEffect, "true, global_b side effect")

    return "OK"
}
