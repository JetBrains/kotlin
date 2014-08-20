package foo

fun box(): String {
    var s = "abc"
    assertEquals("ABC", s.(String::toUpperCase)())

    return "OK"
}
