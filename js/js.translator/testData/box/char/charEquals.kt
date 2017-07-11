// EXPECTED_REACHABLE_NODES: 994
package foo

class A

fun box(): String {

    assertEquals(true, 'A' == 'A')
    assertEquals(false, 'A'== 'B')
    assertEquals(false, ('A' as Any) == (65 as Any))

    return "OK"
}