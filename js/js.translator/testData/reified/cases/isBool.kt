package foo

// CHECK_NOT_CALLED: isInstance

fun box(): String {
    assertEquals(true, isInstance<Boolean>(true))
    assertEquals(true, isInstance<Boolean>(false))
    assertEquals(false, isInstance<Boolean>("true"))

    return "OK"
}