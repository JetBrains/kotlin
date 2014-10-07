package foo

class A

fun box(): String {

    assertEquals(true, 'A' == 'A')
    assertEquals(false, 'A'== 'B')
    assertEquals(false, ('A': Any) == (65: Any))

    return "OK"
}