// EXPECTED_REACHABLE_NODES: 1288
package foo

class A {
    override fun toString() = "42"
}

class B

fun box(): String {
    assertEquals(A().toString(), "42")
    assertEquals(B().toString(), "[object Object]")
    assertEquals(js("\"\"").toString(), "")
    assertEquals(js("123").toString(), "123")
    assertEquals(123.toString(), "123")
    assertEquals("123".toString(), "123")
    assertEquals((123 as Any).toString(), "123")
    assertEquals(null.toString(),  "null")
    return "OK"
}