// EXPECTED_REACHABLE_NODES: 494
external class A

external object O

fun box(): String {
    assertEquals(null, A::class.simpleName, "simpleName of external class must be null")
    assertEquals(js("A"), A::class.js, "Can't get reference to external class")

    assertEquals(null, O::class.simpleName, "simpleName of external object must be null")
    assertEquals(js("O"), O::class.js, "Can't get reference to external object via instance")

    return "OK"
}