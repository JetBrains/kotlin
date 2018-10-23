// EXPECTED_REACHABLE_NODES: 1282
package foo

fun box(): String {
    val number = 3
    val s1 = "${number - 1}${number}"
    val s2 = "${5}${4}"
    assertEquals("2354", "${s1}${s2}")
    return "OK"
}

