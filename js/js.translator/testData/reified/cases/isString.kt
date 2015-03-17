package foo

// CHECK_NOT_CALLED: isInstance

fun box(): String {
    assertEquals(true, isInstance<String>(""))
    assertEquals(true, isInstance<String>("a"))

    return "OK"
}