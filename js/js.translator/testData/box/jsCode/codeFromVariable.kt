// EXPECTED_REACHABLE_NODES: 491
package foo

fun box(): String {
    var c: Int = 0

    val code = "c = 3"
    js(code)

    assertEquals(3, c)
    js(("c = 5"))

    assertEquals(5, c)
    return "OK"
}