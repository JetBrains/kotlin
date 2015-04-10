package foo

fun box(): String {
    assertEquals(2, array(1, fizz(2))[fizz(0) + buzz(1)])
    assertEquals("fizz(2);fizz(0);buzz(1);", pullLog())

    return "OK"
}