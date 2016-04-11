fun box(): String {
    var module1 = bar()
    assertEquals("bar", module1)

    return "OK"
}