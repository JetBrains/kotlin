package foo

fun box(): String {
    assertEquals(array(array(1, 2), array(3, 4)), array(array(fizz(1), buzz(2)), array(fizz(3), buzz(4))))
    assertEquals("fizz(1);buzz(2);fizz(3);buzz(4);", pullLog())

    return "OK"
}