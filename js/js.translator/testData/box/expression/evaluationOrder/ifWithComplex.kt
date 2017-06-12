// EXPECTED_REACHABLE_NODES: 493
package foo

var sideEffect: Int = 0;

fun id(value: Boolean): Boolean = value

fun box(): String {

    assertEquals(if (id(true)) try { ++sideEffect; 10 } finally {} else 20, 10)
    assertEquals(1, sideEffect)

    assertEquals(if (id(false)) try { ++sideEffect; 10 } finally {} else 20, 20)
    assertEquals(1, sideEffect)

    assertEquals(if (id(true)) 100 else try { ++sideEffect; 200 } finally {}, 100)
    assertEquals(1, sideEffect)

    assertEquals(if (id(false)) 100 else try { ++sideEffect; 200 } finally {}, 200)
    assertEquals(2, sideEffect)

    assertEquals(if (id(true)) try { ++sideEffect; 1000 } finally {} else try { ++sideEffect; 2000 } finally {}, 1000)
    assertEquals(3, sideEffect)

    assertEquals(if (id(false)) try { ++sideEffect; 1000 } finally {} else try { ++sideEffect; 2000 } finally {}, 2000)
    assertEquals(4, sideEffect)

    return "OK"
}
