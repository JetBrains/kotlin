package foo

fun box(): String {
    assertEquals(array(1, 2, 3, 4), array(fizz(1), buzz(2), fizz(3), buzz(4)))
    assertEquals("fizz(1);buzz(2);fizz(3);buzz(4);", pullLog())

    return "OK"
}