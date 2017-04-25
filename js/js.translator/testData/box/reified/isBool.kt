// EXPECTED_REACHABLE_NODES: 491
package foo

// CHECK_NOT_CALLED: isInstance

fun box(): String {
    assertEquals(true, isInstance<Boolean>(true))
    assertEquals(true, isInstance<Boolean>(false))
    assertEquals(false, isInstance<Boolean>("true"))

    assertEquals(true, isInstance<Boolean?>(true), "isInstance<Boolean?>(true)")
    assertEquals(true, isInstance<Boolean?>(null), "isInstance<Boolean?>(null)")
    assertEquals(false, isInstance<Boolean?>("true"), "isInstance<Boolean?>(\"true\")")

    return "OK"
}