// EXPECTED_REACHABLE_NODES: 491
package foo

fun box(): String {

    assertEquals("1", "" + ('1' ?: return "fail1"))
    assertEquals("2", "" + (null ?: '2'))
    val c3: Char? = '3'
    assertEquals("3", "" + (c3 ?: return "fail3"))

    val c4: Char? = null
    assertEquals("4", "" + (c4 ?: "4"))

    return "OK"
}