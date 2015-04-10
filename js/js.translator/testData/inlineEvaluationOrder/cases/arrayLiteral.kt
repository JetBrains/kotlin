package foo

fun box(): String {
    assertEquals(array(1, 2), array(fizz(1), buzz(2)))
    assertEquals("fizz(1);buzz(2);", pullLog())

    return "OK"
}