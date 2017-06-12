// EXPECTED_REACHABLE_NODES: 492
package foo

class A

fun box(): String {

    assertEquals(false, ('A' as Any) is Int)
    assertEquals(false, ('A' as Any) is Short)
    assertEquals(false, ('A' as Any) is Byte)
    assertEquals(false, ('A' as Any) is Float)
    assertEquals(false, ('A' as Any) is Double)
    assertEquals(false, ('A' as Any) is Number)

    assertEquals(true, 'A' is Char)
    assertEquals(true, ('A' as Any) is Char)

    return "OK"
}