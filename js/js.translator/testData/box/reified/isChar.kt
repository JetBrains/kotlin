// EXPECTED_REACHABLE_NODES: 491
package foo

// CHECK_NOT_CALLED: isInstance

fun box(): String {
    assertEquals(true, isInstance<Char>('c'))
    assertEquals(false, isInstance<Char>(""))
    assertEquals(false, isInstance<Char>("cc"))

    assertEquals(true, isInstance<Char?>('c'), "isInstance<Char?>('c')")
    assertEquals(true, isInstance<Char?>(null), "isInstance<Char?>(null)")
    assertEquals(false, isInstance<Char?>("cc"), "isInstance<Char?>(\"cc\")")

    return "OK"
}