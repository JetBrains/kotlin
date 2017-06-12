// EXPECTED_REACHABLE_NODES: 498
package foo

fun box(): String {
    assertEquals(eval("undefined"), undefined)
    assertEquals(js("undefined"), undefined)

    assertNotEquals(1, undefined)
    assertNotEquals("sss", undefined)
    assertNotEquals(object {}, undefined)

    val a: dynamic = 1
    assertEquals(a.foo, undefined)
    assertNotEquals(a.toString, undefined)

    val b: dynamic = object {val bar = ""}
    assertEquals(b.foo, undefined)
    assertNotEquals(b.bar, undefined)

    return "OK"
}