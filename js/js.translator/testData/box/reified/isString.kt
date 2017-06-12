// EXPECTED_REACHABLE_NODES: 491
package foo

// CHECK_NOT_CALLED: isInstance

fun box(): String {
    assertEquals(true, isInstance<String>(""))
    assertEquals(true, isInstance<String>("a"))

    assertEquals(true, isInstance<String?>(""), "isInstance<String?>(\"\")")
    assertEquals(true, isInstance<String?>(null), "isInstance<String?>(null)")
    assertEquals(false, isInstance<String?>(10), "isInstance<String?>(10)")

    return "OK"
}