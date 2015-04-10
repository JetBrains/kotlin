package foo

fun box(): String {
    assertEquals(2, fizz(array(1, 2))[buzz(1)])
    assertEquals("fizz(1,2);buzz(1);", pullLog())

    return "OK"
}