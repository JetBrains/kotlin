fun box(): String {

    assertEquals("A: invoked from module", f("A"))
    assertEquals(10, A(10).x)

    return "OK"
}