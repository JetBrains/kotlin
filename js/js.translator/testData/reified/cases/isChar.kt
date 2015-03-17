package foo

// CHECK_NOT_CALLED: isInstance

fun box(): String {
    assertEquals(true, isInstance<Char>('c'))
    assertEquals(false, isInstance<Char>(""))
    assertEquals(false, isInstance<Char>("cc"))

    return "OK"
}