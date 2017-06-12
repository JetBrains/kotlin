// EXPECTED_REACHABLE_NODES: 539
package foo

fun box(): String {

    assertEquals(true, 2..1 == 4..2, "2..1 == 4..2")
    assertEquals(true, 2L..1L == 318238945677L..2L, "2L..1L == 318238945677L..2L")
    assertEquals(false, (2..1) as Any == (4L..2L) as Any, "(2..1): Any == (4L..2L): Any")
    assertEquals(true, 'B'..'A' == 'W'..'B', "'B'..'A' == 'W'..'B'")
    assertEquals(false, (2..1) as Any == ('B'..'A') as Any, "(2..1): Any == ('B'..'A'): Any")

    return "OK"
}