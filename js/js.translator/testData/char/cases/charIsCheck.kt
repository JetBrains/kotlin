package foo

class A

fun box(): String {

    assertEquals(false, ('A': Any) is Int)
    assertEquals(false, ('A': Any) is Short)
    assertEquals(false, ('A': Any) is Byte)
    assertEquals(false, ('A': Any) is Float)
    assertEquals(false, ('A': Any) is Double)
    assertEquals(false, ('A': Any) is Number)

    assertEquals(true, 'A' is Char)
    assertEquals(true, ('A': Any) is Char)

    return "OK"
}