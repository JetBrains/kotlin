package foo

// CHECK_NOT_CALLED: bar

fun box(): String {
    assertEquals(10, bar())
    assertEquals(11, bar(1))

    return "OK"
}