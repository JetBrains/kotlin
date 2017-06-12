// EXPECTED_REACHABLE_NODES: 497
package foo

interface A

class B

fun box(): String {
    assertEquals(false, (23 as Any) is A)
    assertEquals(false, (23 as Any) is B)
    assertEquals(false, (23L as Any) is A)
    assertEquals(false, (23L as Any) is B)
    assertEquals(false, ("qwe" as Any) is A)
    assertEquals(false, ("qwe" as Any) is B)
    assertEquals(false, ({ 23 } as Any) is A)
    assertEquals(false, ({ 23 } as Any) is B)
    assertEquals(false, (true as Any) is A)
    assertEquals(false, (true as Any) is B)
    return "OK"
}